package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Includes deleted accounts but never restores them: restoration happens in AuthenticationService.authenticate,
    // after the password and the account status are validated
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIncludingDeleted(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return toUserDetails(user);
    }

    // Used by the JWT filter on every request, with the user id from the token subject. No side effects:
    // findById doesn't see deleted accounts, so they are rejected, never restored.
    // Ban/suspension is checked by the filter itself, since it answers 403 instead of 401.
    @Transactional
    public RequestUser loadUserForRequest(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        // Built inside the transaction: userInfo and role are lazy
        return new RequestUser(user, toUserDetails(user));
    }

    public record RequestUser(User user, UserDetails userDetails) {}

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserInfo().getEmail())
                .password(user.getUserInfo().getPassword())
                .authorities(getAuthorities(user))
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().toUpperCase())
        );
    }
}