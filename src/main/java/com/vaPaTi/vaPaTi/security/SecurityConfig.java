package com.vaPaTi.vaPaTi.security;

import com.vaPaTi.vaPaTi.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String USER_PATERNS = "/users/**";
    private static final String ADMIN = "ADMIN";
    private static final String USER = "USER";

    // Public URLs that do not require authentication
    private static final String[] PUBLIC_URLS = {
            "/auth/**",               // Authentication routes
            "/api/categories/list",     // Category routes
            "/api/users/create",      // User creation route
            "/swagger-ui/**",         // Swagger interface
            "/swagger-ui.html",       // Swagger home page
            "/v3/api-docs/**",        // OpenAPI documentation
            "/swagger-resources/**",  // Swagger resources
            "/webjars/**",            // Swagger JS/CSS files
            "/favicon.ico",           // Favicon
            "/error"                  // Error page
    };

    // Public GET-only URLs. The services resolve the caller as optional and hide CLOSED campaigns of others
    private static final String[] PUBLIC_GET_URLS = {
            "/api/campaigns/list",
            "/api/campaigns/*",
            "/api/donations/campaign/*",
            "/api/donations/campaign/*/statistics"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(@NotNull HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to the defined URLs
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // my-campaigns also matches /api/campaigns/*, so it must stay authenticated before the public GET rule
                        .requestMatchers(HttpMethod.GET, "/api/campaigns/my-campaigns").authenticated()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()
                        // Specific configuration for users
                        .requestMatchers(HttpMethod.GET, USER_PATERNS).hasAnyRole(USER, ADMIN)
                        .requestMatchers(HttpMethod.POST, USER_PATERNS).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, USER_PATERNS).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, USER_PATERNS).hasRole(ADMIN)
                        // Reports are protected via @PreAuthorize annotations in ReportController
                        // - POST /api/reports - authenticated users
                        // - GET /api/reports/my-reports - authenticated users
                        // - GET /api/reports, GET /api/reports/{id}, PUT /api/reports/{id}/review, GET /api/reports/stats - ADMIN only
                        // Any other request requires authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            String jsonResponse = """
                {
                    "error": "Unauthorized",
                    "message": "Full authentication is required to access this resource",
                    "timestamp": "%s",
                    "path": "%s"
                }
                """.formatted(LocalDateTime.now().toString(), request.getRequestURI());

            response.getWriter().write(jsonResponse);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            String jsonResponse = """
                {
                    "error": "Forbidden",
                    "message": "You don't have permission to access this resource",
                    "timestamp": "%s",
                    "path": "%s"
                }
                """.formatted(LocalDateTime.now().toString(), request.getRequestURI());

            response.getWriter().write(jsonResponse);
        };
    }
}