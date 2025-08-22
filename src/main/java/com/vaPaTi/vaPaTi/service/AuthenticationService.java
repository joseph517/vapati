package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.AuthRequest;
import com.vaPaTi.vaPaTi.dtos.AuthResponse;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse authenticate(@NotNull AuthRequest request) {
        
        try {
            // Use AuthenticationManager to validate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new MessageException("Invalid email or password");
        }

        // If authentication is successful, find the user
        User user = userRepository.findAllWithDetails().stream()
                .filter(u -> u.getUserInfo().getEmail().equalsIgnoreCase(request.getEmail()))
                .findFirst()
                .orElseThrow(() -> new MessageException("User not found"));

        if (!user.isActive()) {
            throw new MessageException("User account is disabled");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.fromUser(user);

        return new AuthResponse(accessToken, refreshToken, userInfo);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new MessageException("Invalid or expired refresh token");
        }

        try {
            String email = jwtService.extractUsername(refreshToken);

            if (jwtService.isTokenValid(refreshToken, email)) {
                User user = userRepository.findAllWithDetails().stream()
                        .filter(u -> u.getUserInfo().getEmail().equalsIgnoreCase(email))
                        .findFirst()
                        .orElseThrow(() -> new MessageException("User not found"));

                if (!user.isActive()) {
                    throw new MessageException("User account is disabled");
                }

                String newAccessToken = jwtService.generateToken(user);
                String newRefreshToken = jwtService.generateRefreshToken(user);

                AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.fromUser(user);

                return new AuthResponse(newAccessToken, newRefreshToken, userInfo);
            } else {
                throw new MessageException("Invalid or expired refresh token");
            }
        } catch (MessageException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MessageException("Invalid or expired refresh token");
        }
    }

}