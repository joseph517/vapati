package com.vaPaTi.vaPaTi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.service.CustomUserDetailsService;
import com.vaPaTi.vaPaTi.service.CustomUserDetailsService.RequestUser;
import com.vaPaTi.vaPaTi.service.JwtService;
import com.vaPaTi.vaPaTi.service.TokenBlackListService;
import com.vaPaTi.vaPaTi.validation.AccountStatusValidationService;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "mySecretKeyForTestingThatIsLongEnoughForHS256Algorithm";
    private static final String EMAIL = "john.doe@example.com";
    private static final Long USER_ID = 1L;

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

        filter = new JwtAuthenticationFilter(jwtService, tokenBlackListService, userDetailsService, new AccountStatusValidationService());

        UserInfo userInfo = UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .email(EMAIL)
                .build();
        user = User.builder()
                .id(USER_ID)
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

    // Format before spec 21: access token with the email as subject
    private String oldFormatAccessToken() {
        return Jwts.builder()
                .setSubject(EMAIL)
                .claim("userId", USER_ID)
                .claim("type", JwtService.ACCESS_TOKEN_TYPE)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
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
            when(userDetailsService.loadUserForRequest(USER_ID)).thenReturn(new RequestUser(user, userDetails()));
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
            verify(userDetailsService, never()).loadUserForRequest(anyLong());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            // The entry point writes the 401 later; the filter must not commit an error itself
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Old-format access token (sub = email) leaves the request unauthenticated")
        void shouldRejectOldFormatAccessToken() throws Exception {
            String oldToken = oldFormatAccessToken();
            when(tokenBlackListService.isTokenRevoked(jwtService.extractJti(oldToken))).thenReturn(false);
            MockHttpServletRequest request = requestWithBearer("/api/campaigns/my-campaigns", oldToken);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(response.getStatus()).isEqualTo(200);
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

    @Nested
    @DisplayName("Account status on every request")
    class AccountStatusTests {

        private MockHttpServletRequest request;
        private MockHttpServletResponse response;

        @BeforeEach
        void setUpRequest() {
            String accessToken = jwtService.generateToken(user);
            when(tokenBlackListService.isTokenRevoked(jwtService.extractJti(accessToken))).thenReturn(false);
            request = requestWithBearer("/api/campaigns/my-campaigns", accessToken);
            response = new MockHttpServletResponse();
        }

        @Test
        @DisplayName("Deleted account: stays unauthenticated (401) and is not restored")
        void shouldRejectDeletedAccount() throws Exception {
            when(userDetailsService.loadUserForRequest(USER_ID))
                    .thenThrow(new UsernameNotFoundException("User account is deleted"));

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Token issued before tokensValidAfter: stays unauthenticated (401), even if banned")
        void shouldRejectTokenIssuedBeforeTokensValidAfter() throws Exception {
            user.setTokensValidAfter(LocalDateTime.now().plusMinutes(1));
            user.setBanned(true);
            when(userDetailsService.loadUserForRequest(USER_ID)).thenReturn(new RequestUser(user, userDetails()));

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Token issued after tokensValidAfter: authenticated")
        void shouldAuthenticateTokenIssuedAfterTokensValidAfter() throws Exception {
            user.setTokensValidAfter(LocalDateTime.now().minusMinutes(1));
            when(userDetailsService.loadUserForRequest(USER_ID)).thenReturn(new RequestUser(user, userDetails()));

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }

        @Test
        @DisplayName("Disabled account: stays unauthenticated (401)")
        void shouldRejectDisabledAccount() throws Exception {
            when(userDetailsService.loadUserForRequest(USER_ID))
                    .thenThrow(new UsernameNotFoundException("User account is disabled"));

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Banned account: 403 with the login message and the SecurityConfig JSON shape")
        void shouldCutBannedAccountWith403() throws Exception {
            user.setBanned(true);
            user.setBannedReason("Spam \"quoted\"");
            when(userDetailsService.loadUserForRequest(USER_ID)).thenReturn(new RequestUser(user, userDetails()));

            filter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getContentType()).startsWith("application/json");

            JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
            assertThat(body.get("error").asText()).isEqualTo("Forbidden");
            assertThat(body.get("message").asText()).isEqualTo("Your account has been banned. Reason: Spam \"quoted\"");
            assertThat(body.get("path").asText()).isEqualTo("/api/campaigns/my-campaigns");
            assertThat(body.hasNonNull("timestamp")).isTrue();
        }

        @Test
        @DisplayName("Suspended account (suspendedUntil in the future): 403")
        void shouldCutSuspendedAccountWith403() throws Exception {
            user.setSuspendedUntil(LocalDateTime.now().plusDays(2));
            when(userDetailsService.loadUserForRequest(USER_ID)).thenReturn(new RequestUser(user, userDetails()));

            filter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(403);
            JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
            assertThat(body.get("message").asText()).startsWith("Your account is suspended until");
        }

        @Test
        @DisplayName("Expired suspension (suspendedUntil in the past): authenticated")
        void shouldAuthenticateWithExpiredSuspension() throws Exception {
            user.setSuspendedUntil(LocalDateTime.now().minusDays(1));
            when(userDetailsService.loadUserForRequest(USER_ID)).thenReturn(new RequestUser(user, userDetails()));

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }
    }
}
