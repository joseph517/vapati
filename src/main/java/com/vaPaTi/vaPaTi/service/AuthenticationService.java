package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.AuthRequest;
import com.vaPaTi.vaPaTi.dtos.AuthResponse;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.InvalidCredentialsException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.validation.AccountStatusValidationService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String INVALID_REFRESH_TOKEN_MSG = "Invalid or expired refresh token";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlackListService tokenBlackListService;
    private final AccountStatusValidationService accountStatusValidationService;

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        if (request == null) {
            throw new MessageException("Authentication request cannot be null");
        }
        
        try {
            // Use AuthenticationManager to validate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // If authentication is successful, find the user
        User user = userRepository.findAllWithDetails().stream()
                .filter(u -> u.getUserInfo().getEmail().equalsIgnoreCase(request.getEmail()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateUserStatus(user);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        validateRefreshToken(refreshToken);

        try {
            String email = jwtService.extractUsername(refreshToken);

            // Only non-expired refresh tokens are accepted (access tokens and tokens without "type" are rejected)
            if (!jwtService.isTokenValid(refreshToken, email) || !jwtService.isRefreshToken(refreshToken)) {
                throw new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MSG);
            }

            String jti = jwtService.extractJti(refreshToken);
            if (tokenBlackListService.isTokenRevoked(jti)) {
                throw new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MSG);
            }

            User user = findUserByEmail(email);
            validateUserStatus(user);

            // Rotation: the used refresh token can't be used again
            revokeUsedRefreshToken(jti, jwtService.extractExpirationDateTime(refreshToken));

            return generateAuthResponse(user);
        } catch (MessageException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }
    }

    // Two concurrent refreshes with the same token both pass the blacklist check; the second insert violates the UNIQUE on jti
    private void revokeUsedRefreshToken(String jti, LocalDateTime expirationDate) {
        try {
            tokenBlackListService.revokeToken(jti, expirationDate);
        } catch (DataIntegrityViolationException e) {
            throw new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }
    }

    // Not @Transactional: each revocation commits on its own, so a UNIQUE violation from a concurrent logout doesn't roll back the other
    public void logout(String refreshToken, String accessToken) {
        validateRefreshToken(refreshToken);

        String refreshJti;
        LocalDateTime refreshExpiration;
        Long userId;
        try {
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MSG);
            }
            refreshJti = jwtService.extractJti(refreshToken);
            refreshExpiration = jwtService.extractExpirationDateTime(refreshToken);
            userId = jwtService.extractUserData(refreshToken).getUserId();
        } catch (ExpiredJwtException e) {
            // An expired refresh token can't be used anymore: logout is idempotent
            return;
        } catch (MessageException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        // Already revoked: logout is idempotent
        if (tokenBlackListService.isTokenRevoked(refreshJti)) {
            return;
        }

        revokeIgnoringDuplicates(refreshJti, refreshExpiration);

        if (accessToken != null) {
            revokeAccessTokenIfOwnedBy(accessToken, userId);
        }
    }

    // The access token is optional: an invalid, expired, foreign or refresh-type token is ignored
    private void revokeAccessTokenIfOwnedBy(String accessToken, Long userId) {
        try {
            if (jwtService.isAccessToken(accessToken)
                    && userId != null
                    && userId.equals(jwtService.extractUserData(accessToken).getUserId())) {
                revokeIgnoringDuplicates(jwtService.extractJti(accessToken), jwtService.extractExpirationDateTime(accessToken));
            }
        } catch (RuntimeException e) {
            // Ignored on purpose: it must not make the logout fail
        }
    }

    // A concurrent logout may have revoked the same jti between the check and the insert
    private void revokeIgnoringDuplicates(String jti, LocalDateTime expirationDate) {
        try {
            tokenBlackListService.revokeToken(jti, expirationDate);
        } catch (DataIntegrityViolationException e) {
            // Already revoked
        }
    }

    private void validateRefreshToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }
    }

    // findAllWithDetails doesn't return deleted users, so a deleted account gets 401 as well
    private User findUserByEmail(String email) {
        return userRepository.findAllWithDetails().stream()
                .filter(u -> u.getUserInfo().getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MSG));
    }

    private void validateUserStatus(User user) {
        if (!user.isActive()) {
            throw new ForbiddenActionException("User account is disabled");
        }

        accountStatusValidationService.validateNotBlocked(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.fromUser(user);
        return new AuthResponse(accessToken, refreshToken, userInfo);
    }

}