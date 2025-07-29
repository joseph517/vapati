package com.vaPaTi.vaPaTi.security;

import com.vaPaTi.vaPaTi.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final JwtService jwtService;

    public Long getAuthenticatedUserId() {
        String token = extractTokenFromContext();
        return jwtService.extractUserData(token).getUserId();
    }

    private String extractTokenFromContext() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getCredentials() instanceof String token)) {
            throw new IllegalStateException("Cannot extract token from SecurityContext");
        }
        return token;
    }
}

