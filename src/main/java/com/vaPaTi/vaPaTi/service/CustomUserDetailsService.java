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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Primero intentar encontrar usuario activo (con la restricción SQL)
        Optional<User> activeUser = userRepository.findAllWithDetails().stream()
                .filter(u -> u.getUserInfo().getEmail().equalsIgnoreCase(email))
                .findFirst();

        if (activeUser.isPresent()) {
            User user = activeUser.get();
            if (!user.isActive()) {
                throw new UsernameNotFoundException("User account is disabled");
            }

            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUserInfo().getEmail())
                    .password(user.getUserInfo().getPassword())
                    .authorities(getAuthorities(user))
                    .disabled(!user.isActive())
                    .build();
        }

        // Si no se encuentra usuario activo, buscar si existe uno eliminado
        Optional<User> deletedUser = userRepository.findByEmailIncludingDeleted(email);

        if (deletedUser.isPresent() && deletedUser.get().getDeletedAt() != null) {
            // Usuario existe pero está eliminado - restaurar automáticamente
            User userToRestore = deletedUser.get();
            userToRestore.setDeletedAt(null);
            userRepository.save(userToRestore);

            // Verificar que el usuario restaurado esté activo
            if (!userToRestore.isActive()) {
                throw new UsernameNotFoundException("User account is disabled");
            }

            return org.springframework.security.core.userdetails.User.builder()
                    .username(userToRestore.getUserInfo().getEmail())
                    .password(userToRestore.getUserInfo().getPassword())
                    .authorities(getAuthorities(userToRestore))
                    .disabled(!userToRestore.isActive())
                    .build();
        }

        // Usuario no existe en absoluto
        throw new UsernameNotFoundException("User not found with email: " + email);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().toUpperCase())
        );
    }
}