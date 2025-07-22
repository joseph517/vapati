package com.vaPaTi.vaPaTi.security;

import com.vaPaTi.vaPaTi.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
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

    private static final String userPaterns = "/users/**";
    private static final String ADMIN = "ADMIN";
    private static final String USER = "USER";

    // URLs públicas que no requieren autenticación
    private static final String[] PUBLIC_URLS = {
            "/auth/**",               // Rutas de autenticación
            "/swagger-ui/**",         // Interfaz Swagger
            "/swagger-ui.html",       // Página principal de Swagger
            "/v3/api-docs/**",        // Documentación OpenAPI
            "/swagger-resources/**",  // Recursos Swagger
            "/webjars/**",            // Archivos JS/CSS de Swagger
            "/favicon.ico",           // Favicon
            "/error"                  // Página de error
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to the defined URLs
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // Specific configuration for users
                        .requestMatchers(HttpMethod.GET, userPaterns).hasAnyRole(USER, ADMIN)
                        .requestMatchers(HttpMethod.POST, userPaterns).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, userPaterns).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, userPaterns).hasRole(ADMIN)
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