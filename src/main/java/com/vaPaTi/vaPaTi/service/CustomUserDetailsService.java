package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findAllWithDetails().stream()
                .filter(u -> u.getUserInfo().getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User account is disabled");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserInfo().getEmail())
                .password(user.getUserInfo().getPassword())
                .authorities(getAuthorities(user))
                .disabled(!user.getIsActive())
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().toUpperCase())
        );
    }
}