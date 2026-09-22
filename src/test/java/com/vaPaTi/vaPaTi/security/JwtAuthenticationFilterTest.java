package com.vaPaTi.vaPaTi.security;

import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.service.CustomUserDetailsService;
import com.vaPaTi.vaPaTi.service.JwtService;
import com.vaPaTi.vaPaTi.service.TokenBlackListService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "mySecretKeyForTestingThatIsLongEnoughForHS256Algorithm";
    private static final String EMAIL = "john.doe@example.com";

    @Mock
    private TokenBlackListService tokenBlackListService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpirationMs", 604800000L);
        jwtService.init();

        filter = new JwtAuthenticationFilter(jwtService, tokenBlackListService, userDetailsService);

        UserInfo userInfo = UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .email(EMAIL)
                .build();
        user = User.builder()
                .id(1L)
                .role(Role.builder().id(1L).name("USER").build())
                .userInfo(userInfo)
                .active(true)
                .build();
        userInfo.setUser(user);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithBearer(String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private UserDetails userDetails() {
        return new org.springframework.security.core.userdetails.User(
                EMAIL, "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private String tokenWithoutType() {
        return Jwts.builder()
                .setSubject(EMAIL)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    @Nested
    @DisplayName("shouldNotFilter()")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("Skips /auth/** routes")
        void shouldSkipAuthRoutes() {
            assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/auth/logout"))).isTrue();
            assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/auth/refresh-token"))).isTrue();
        }

        @Test
        @DisplayName("Processes non-auth routes")
        void shouldProcessOtherRoutes() {
            assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/campaigns/my-campaigns"))).isFalse();
        }

        @Test
        @DisplayName("Does not read the token on /auth/** even with an Authorization header")
        void shouldNotTouchTokenOnAuthRoutes() throws Exception {
            MockHttpServletRequest request = requestWithBearer("/auth/logout", jwtService.generateToken(user));
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenBlackListService, userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("doFilterInternal()")
    class DoFilterInternalTests {

        @Test
        @DisplayName("Without Authorization header continues unauthenticated")
        void shouldContinueWithoutHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/campaigns/my-campaigns");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Valid, non-revoked access token authenticates the request")
        void shouldAuthenticateWithAccessToken() throws Exception {
            String accessToken = jwtService.generateToken(user);
            when(tokenBlackListService.isTokenRevoked(jwtService.extractJti(accessToken))).thenReturn(false);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails());
            MockHttpServletRequest request = requestWithBearer("/api/campaigns/my-campaigns", accessToken);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("Refresh token used as Bearer leaves the request unauthenticated")
        void shouldRejectRefreshToken() throws Exception {
            MockHttpServletRequest request = requestWithBearer("/api/campaigns/my-campaigns", jwtService.generateRefreshToken(user));
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenBlackListService, userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Token without type claim leaves the request unauthenticated")
        void shouldRejectTokenWithoutType() throws Exception {
            MockHttpServletRequest request = requestWithBearer("/api/campaigns/my-campaigns", tokenWithoutType());
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenBlackListService, userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Revoked access token leaves the request unauthenticated without sendError")
        void shouldRejectRevokedAccessToken() throws Exception {
            String accessToken = jwtService.generateToken(user);
            when(tokenBlackListService.isTokenRevoked(jwtService.extractJti(accessToken))).thenReturn(true);
            MockHttpServletRequest request = requestWithBearer("/api/campaigns/my-campaigns", accessToken);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            // The entry point writes the 401 later; the filter must not commit an error itself
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Malformed token leaves the request unauthenticated")
        void shouldRejectMalformedToken() throws Exception {
            MockHttpServletRequest request = requestWithBearer("/api/campaigns/my-campaigns", "invalid.jwt.token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenBlackListService, userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
