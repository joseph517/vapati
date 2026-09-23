package com.vaPaTi.vaPaTi.security;

import com.vaPaTi.vaPaTi.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final JwtService jwtService;

    public Long getAuthenticatedUserId() {
        String token = extractTokenFromContext();
        return jwtService.extractUserData(token).getUserId();
    }

    // For public routes: empty when the caller is anonymous instead of failing
    public Optional<Long> findAuthenticatedUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // Spring puts an AnonymousAuthenticationToken (with empty String credentials) on permitAll routes without a JWT
        if (auth == null || auth instanceof AnonymousAuthenticationToken
                || !(auth.getCredentials() instanceof String token) || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(jwtService.extractUserData(token).getUserId());
    }

    private String extractTokenFromContext() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getCredentials() instanceof String token)) {
            throw new IllegalStateException("Cannot extract token from SecurityContext");
        }
        return token;
    }
}
