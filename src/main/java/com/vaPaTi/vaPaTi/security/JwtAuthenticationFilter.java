package com.vaPaTi.vaPaTi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.service.CustomUserDetailsService;
import com.vaPaTi.vaPaTi.service.CustomUserDetailsService.RequestUser;
import com.vaPaTi.vaPaTi.service.JwtService;
import com.vaPaTi.vaPaTi.service.TokenBlackListService;
import com.vaPaTi.vaPaTi.validation.AccountStatusValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlackListService tokenBlackListService;
    private final CustomUserDetailsService userDetailsService;
    private final AccountStatusValidationService accountStatusValidationService;

    private static final String AUTH_PATH_PREFIX = "/auth/";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // /auth/** endpoints read their own tokens (login, refresh, logout)
    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith(AUTH_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Any rejected token leaves the request unauthenticated; the entry point answers 401 if the route requires it
        try {
            final String jwt = authHeader.substring(7);

            // Only access tokens can authenticate requests (tokens without "type" are rejected)
            if (!jwtService.isAccessToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check if the token is in the blacklist (by its jti)
            if (tokenBlackListService.isTokenRevoked(jwtService.extractJti(jwt))) {
                filterChain.doFilter(request, response);
                return;
            }

            // A non-numeric subject (old tokens, sub = email) throws and falls into the catch below (401)
            final Long userId = jwtService.extractSubjectUserId(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Missing, deleted or disabled accounts throw here and stay unauthenticated (401)
                RequestUser requestUser = this.userDetailsService.loadUserForRequest(userId);

                // Banned or suspended accounts are cut with 403, with the same message as the login
                try {
                    accountStatusValidationService.validateNotBlocked(requestUser.user());
                } catch (ForbiddenActionException e) {
                    writeForbidden(request, response, e.getMessage());
                    return;
                }

                UserDetails userDetails = requestUser.userDetails();
                if (jwtService.isTokenValid(jwt, userId)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    jwt,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log the error without exposing details
            System.err.println("Cannot set user authentication: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    // Same {error, message, timestamp, path} shape as the entry point and access denied handler in SecurityConfig
    private void writeForbidden(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", "Forbidden");
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("path", request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }
}
