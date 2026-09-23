package com.vaPaTi.vaPaTi.service.Authentication;

import com.vaPaTi.vaPaTi.dtos.AuthResponse;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.InvalidCredentialsException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.service.AuthenticationService;
import com.vaPaTi.vaPaTi.service.JwtService;
import com.vaPaTi.vaPaTi.service.TokenBlackListService;
import com.vaPaTi.vaPaTi.validation.AccountStatusValidationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshToken Service Tests")
class RefreshTokenServiceTest {

    private static final String INVALID_REFRESH_TOKEN_MSG = "Invalid or expired refresh token";

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenBlackListService tokenBlackListService;

    private AuthenticationService authService;

    // Test data
    private String validRefreshToken;
    private String refreshJti;
    private LocalDateTime refreshExpiration;
    private String userEmail;
    private String newAccessToken;
    private String newRefreshToken;
    private User activeUser;
    private User inactiveUser;
    private UserInfo userInfo;
    private Role userRole;

    @BeforeEach
    void setUp() {
        authService = new AuthenticationService(userRepository, jwtService, authenticationManager, tokenBlackListService, new AccountStatusValidationService());

        // Setup test data
        validRefreshToken = "valid.refresh.token";
        refreshJti = "3f1c9a7e-2b4d-4c8e-9f61-0a5b7d2e8c14";
        refreshExpiration = LocalDateTime.now().plusDays(7);
        userEmail = "user@example.com";
        newAccessToken = "new.access.token";
        newRefreshToken = "new.refresh.token";

        // Create test role
        userRole = Role.builder()
                .id(1L)
                .name("USER")
                .build();

        // Create test user info
        userInfo = UserInfo.builder()
                .email(userEmail)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        // Create active user
        activeUser = User.builder()
                .id(1L)
                .userInfo(userInfo)
                .role(userRole)
                .active(true)
                .build();

        // Create inactive user
        inactiveUser = User.builder()
                .id(2L)
                .userInfo(userInfo)
                .role(userRole)
                .active(false)
                .build();
    }

    // Stubs a signed, non-expired, non-revoked refresh token whose subject is the given user id
    private void stubUsableRefreshToken(Long userId) {
        when(jwtService.extractSubjectUserId(validRefreshToken)).thenReturn(userId);
        when(jwtService.isTokenValid(validRefreshToken, userId)).thenReturn(true);
        when(jwtService.isRefreshToken(validRefreshToken)).thenReturn(true);
        when(jwtService.extractJti(validRefreshToken)).thenReturn(refreshJti);
        when(tokenBlackListService.isTokenRevoked(refreshJti)).thenReturn(false);
    }

    private void stubRotation() {
        when(jwtService.extractExpirationDateTime(validRefreshToken)).thenReturn(refreshExpiration);
    }

    @Test
    @DisplayName("Should successfully refresh token and revoke the used one when all conditions are met")
    void shouldSuccessfullyRefreshTokenWhenAllConditionsAreMet() {
        // Given
        stubUsableRefreshToken(activeUser.getId());
        stubRotation();
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(jwtService.generateToken(activeUser)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(activeUser)).thenReturn(newRefreshToken);

        // When
        AuthResponse result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(newRefreshToken, result.getRefreshToken());
        assertNotNull(result.getUserInfo());
        assertEquals(activeUser.getId(), result.getUserInfo().userId);
        assertEquals(userEmail, result.getUserInfo().email);
        assertEquals("USER", result.getUserInfo().role);
        assertEquals("John", result.getUserInfo().firstName);
        assertEquals("Doe", result.getUserInfo().lastName);
        assertEquals("johndoe", result.getUserInfo().userName);
        assertEquals("John Doe", result.getUserInfo().fullName);

        // Verify method calls order: validate type, check blacklist, load user, revoke used token, then issue new pair
        InOrder inOrder = inOrder(jwtService, tokenBlackListService, userRepository);
        inOrder.verify(jwtService).isRefreshToken(validRefreshToken);
        inOrder.verify(tokenBlackListService).isTokenRevoked(refreshJti);
        inOrder.verify(userRepository).findById(activeUser.getId());
        inOrder.verify(tokenBlackListService).revokeToken(refreshJti, refreshExpiration);
        inOrder.verify(jwtService).generateToken(activeUser);
        inOrder.verify(jwtService).generateRefreshToken(activeUser);
    }

    @Test
    @DisplayName("Should throw MessageException when refresh token is null")
    void shouldThrowMessageExceptionWhenRefreshTokenIsNull() {
        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(null)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verifyNoInteractions(jwtService, userRepository, authenticationManager, tokenBlackListService);
    }

    @Test
    @DisplayName("Should throw MessageException when refresh token is empty string")
    void shouldThrowMessageExceptionWhenRefreshTokenIsEmptyString() {
        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken("")
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verifyNoInteractions(jwtService, userRepository, authenticationManager, tokenBlackListService);
    }

    @Test
    @DisplayName("Should throw MessageException when refresh token is only whitespace")
    void shouldThrowMessageExceptionWhenRefreshTokenIsOnlyWhitespace() {
        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken("   \t\n   ")
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verifyNoInteractions(jwtService, userRepository, authenticationManager, tokenBlackListService);
    }

    @Test
    @DisplayName("Should throw MessageException when token is invalid")
    void shouldThrowMessageExceptionWhenTokenIsInvalid() {
        // Given
        when(jwtService.extractSubjectUserId(validRefreshToken)).thenReturn(activeUser.getId());
        when(jwtService.isTokenValid(validRefreshToken, activeUser.getId())).thenReturn(false);

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verifyNoInteractions(userRepository, tokenBlackListService);
    }

    @Test
    @DisplayName("Should reject a token that is not of type refresh with 401")
    void shouldRejectNonRefreshToken() {
        // Given
        when(jwtService.extractSubjectUserId(validRefreshToken)).thenReturn(activeUser.getId());
        when(jwtService.isTokenValid(validRefreshToken, activeUser.getId())).thenReturn(true);
        when(jwtService.isRefreshToken(validRefreshToken)).thenReturn(false);

        // When & Then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verifyNoInteractions(userRepository, tokenBlackListService);
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should reject a revoked refresh token with 401")
    void shouldRejectRevokedRefreshToken() {
        // Given
        when(jwtService.extractSubjectUserId(validRefreshToken)).thenReturn(activeUser.getId());
        when(jwtService.isTokenValid(validRefreshToken, activeUser.getId())).thenReturn(true);
        when(jwtService.isRefreshToken(validRefreshToken)).thenReturn(true);
        when(jwtService.extractJti(validRefreshToken)).thenReturn(refreshJti);
        when(tokenBlackListService.isTokenRevoked(refreshJti)).thenReturn(true);

        // When & Then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verifyNoInteractions(userRepository);
        verify(tokenBlackListService, never()).revokeToken(any(), any());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should respond 401 (not 404) when the user is not found or deleted")
    void shouldThrowInvalidCredentialsWhenUserIsNotFound() {
        // Given - findById doesn't return deleted users
        stubUsableRefreshToken(activeUser.getId());
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.empty());

        // When & Then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verify(tokenBlackListService, never()).revokeToken(any(), any());
    }

    @Test
    @DisplayName("Should respond 401 when the refresh token was issued before tokensValidAfter")
    void shouldThrowInvalidCredentialsWhenIssuedBeforeTokensValidAfter() {
        // Given - the email or password changed after the token was issued
        LocalDateTime tokensValidAfter = LocalDateTime.now();
        activeUser.setTokensValidAfter(tokensValidAfter);
        stubUsableRefreshToken(activeUser.getId());
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(jwtService.isIssuedBefore(validRefreshToken, tokensValidAfter)).thenReturn(true);

        // When & Then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verify(tokenBlackListService, never()).revokeToken(any(), any());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).generateRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("Should respond 401 for an old-format refresh token (sub = email)")
    void shouldThrowInvalidCredentialsForOldFormatSubject() {
        // Given - the subject is not numeric
        when(jwtService.extractSubjectUserId(validRefreshToken))
                .thenThrow(new NumberFormatException("For input string: \"user@example.com\""));

        // When & Then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verifyNoInteractions(userRepository, tokenBlackListService);
    }

    @Test
    @DisplayName("Should refresh by id even if the email changed after the token was issued")
    void shouldRefreshByIdEvenIfEmailChanged() {
        // Given - the session doesn't depend on the email
        userInfo.setEmail("changed@example.com");
        stubUsableRefreshToken(activeUser.getId());
        stubRotation();
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(jwtService.generateToken(activeUser)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(activeUser)).thenReturn(newRefreshToken);

        // When
        AuthResponse result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(newRefreshToken, result.getRefreshToken());
        assertEquals("changed@example.com", result.getUserInfo().email);
    }

    @Test
    @DisplayName("Should throw MessageException when user account is disabled")
    void shouldThrowMessageExceptionWhenUserAccountIsDisabled() {
        // Given
        stubUsableRefreshToken(inactiveUser.getId());
        when(userRepository.findById(inactiveUser.getId())).thenReturn(Optional.of(inactiveUser));

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals("User account is disabled", exception.getMessage());
        verify(tokenBlackListService, never()).revokeToken(any(), any());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).generateRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("Should respond 403 when the user is banned")
    void shouldThrowForbiddenWhenUserIsBanned() {
        // Given
        activeUser.setBanned(true);
        activeUser.setBannedReason("Spam");
        stubUsableRefreshToken(activeUser.getId());
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        // When & Then
        ForbiddenActionException exception = assertThrows(
                ForbiddenActionException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertTrue(exception.getMessage().startsWith("Your account has been banned"));
        verify(tokenBlackListService, never()).revokeToken(any(), any());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should respond 403 when the user is suspended")
    void shouldThrowForbiddenWhenUserIsSuspended() {
        // Given
        activeUser.setSuspendedUntil(LocalDateTime.now().plusDays(3));
        stubUsableRefreshToken(activeUser.getId());
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        // When & Then
        ForbiddenActionException exception = assertThrows(
                ForbiddenActionException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertTrue(exception.getMessage().startsWith("Your account is suspended until"));
        verify(tokenBlackListService, never()).revokeToken(any(), any());
    }

    @Test
    @DisplayName("Should respond 401 when a concurrent refresh already revoked the same jti (UNIQUE violation)")
    void shouldThrowInvalidCredentialsOnConcurrentRevocation() {
        // Given
        stubUsableRefreshToken(activeUser.getId());
        stubRotation();
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        doThrow(new DataIntegrityViolationException("Violation of UNIQUE KEY constraint"))
                .when(tokenBlackListService).revokeToken(refreshJti, refreshExpiration);

        // When & Then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).generateRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("Should throw MessageException when JWT service throws exception during token extraction")
    void shouldThrowMessageExceptionWhenJwtServiceThrowsExceptionDuringTokenExtraction() {
        // Given
        when(jwtService.extractSubjectUserId(validRefreshToken))
                .thenThrow(new RuntimeException("JWT parsing error"));

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verifyNoInteractions(userRepository, tokenBlackListService);
    }

    @Test
    @DisplayName("Should throw MessageException when JWT service throws exception during token validation")
    void shouldThrowMessageExceptionWhenJwtServiceThrowsExceptionDuringTokenValidation() {
        // Given
        when(jwtService.extractSubjectUserId(validRefreshToken)).thenReturn(activeUser.getId());
        when(jwtService.isTokenValid(validRefreshToken, activeUser.getId()))
                .thenThrow(new RuntimeException("Token validation error"));

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verifyNoInteractions(userRepository, tokenBlackListService);
    }

    @Test
    @DisplayName("Should throw MessageException when user repository throws exception")
    void shouldThrowMessageExceptionWhenUserRepositoryThrowsException() {
        // Given
        stubUsableRefreshToken(activeUser.getId());
        when(userRepository.findById(activeUser.getId()))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verify(tokenBlackListService, never()).revokeToken(any(), any());
    }

    @Test
    @DisplayName("Should throw MessageException when token generation throws exception")
    void shouldThrowMessageExceptionWhenTokenGenerationThrowsException() {
        // Given
        stubUsableRefreshToken(activeUser.getId());
        stubRotation();
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(jwtService.generateToken(activeUser))
                .thenThrow(new RuntimeException("Token generation error"));

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verify(jwtService, never()).generateRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("Should load the user by id without scanning all users")
    void shouldLoadUserByIdWithoutScanningAllUsers() {
        // Given
        stubUsableRefreshToken(activeUser.getId());
        stubRotation();
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(jwtService.generateToken(activeUser)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(activeUser)).thenReturn(newRefreshToken);

        // When
        AuthResponse result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(activeUser.getId(), result.getUserInfo().userId);
        verify(userRepository).findById(activeUser.getId());
        verify(userRepository, never()).findAllWithDetails();
    }

    @Test
    @DisplayName("Should create UserInfo with correct full name concatenation")
    void shouldCreateUserInfoWithCorrectFullNameConcatenation() {
        // Given
        stubUsableRefreshToken(activeUser.getId());
        stubRotation();
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(jwtService.generateToken(activeUser)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(activeUser)).thenReturn(newRefreshToken);

        // When
        AuthResponse result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result.getUserInfo());
        assertEquals("John Doe", result.getUserInfo().fullName);
        assertEquals("John", result.getUserInfo().firstName);
        assertEquals("Doe", result.getUserInfo().lastName);
    }

    @Nested
    @DisplayName("With real signed tokens")
    class RealTokenTests {

        private static final String TEST_SECRET = "mySecretKeyForTestingThatIsLongEnoughForHS256Algorithm";

        private JwtService realJwtService;
        private AuthenticationService realAuthService;

        @BeforeEach
        void setUpRealJwt() {
            realJwtService = new JwtService();
            ReflectionTestUtils.setField(realJwtService, "jwtSecret", TEST_SECRET);
            ReflectionTestUtils.setField(realJwtService, "jwtExpirationMs", 3600000L);
            ReflectionTestUtils.setField(realJwtService, "jwtRefreshExpirationMs", 604800000L);
            realJwtService.init();
            realAuthService = new AuthenticationService(userRepository, realJwtService, authenticationManager, tokenBlackListService, new AccountStatusValidationService());
        }

        @Test
        @DisplayName("An access token sent to refresh responds 401")
        void shouldRejectAccessToken() {
            String accessToken = realJwtService.generateToken(activeUser);

            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> realAuthService.refreshToken(accessToken)
            );

            assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
            verifyNoInteractions(userRepository, tokenBlackListService);
        }

        @Test
        @DisplayName("A token without type claim sent to refresh responds 401")
        void shouldRejectTokenWithoutType() {
            String legacyToken = Jwts.builder()
                    .setSubject(userEmail)
                    .setId(UUID.randomUUID().toString())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()), SignatureAlgorithm.HS256)
                    .compact();

            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> realAuthService.refreshToken(legacyToken)
            );

            assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
            verifyNoInteractions(userRepository, tokenBlackListService);
        }

        @Test
        @DisplayName("An old-format refresh token (sub = email) sent to refresh responds 401")
        void shouldRejectOldFormatRefreshToken() {
            String oldFormatToken = Jwts.builder()
                    .setSubject(userEmail)
                    .claim("userId", activeUser.getId())
                    .claim("type", JwtService.REFRESH_TOKEN_TYPE)
                    .setId(UUID.randomUUID().toString())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()), SignatureAlgorithm.HS256)
                    .compact();

            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> realAuthService.refreshToken(oldFormatToken)
            );

            assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
            verifyNoInteractions(userRepository, tokenBlackListService);
        }

        @Test
        @DisplayName("A real refresh token issued before tokensValidAfter responds 401 and is not rotated")
        void shouldRejectRealRefreshTokenIssuedBeforeTokensValidAfter() {
            String refreshToken = realJwtService.generateRefreshToken(activeUser);
            activeUser.setTokensValidAfter(LocalDateTime.now().plusSeconds(2));
            when(tokenBlackListService.isTokenRevoked(realJwtService.extractJti(refreshToken))).thenReturn(false);
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> realAuthService.refreshToken(refreshToken)
            );

            assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
            verify(tokenBlackListService, never()).revokeToken(any(), any());
        }

        @Test
        @DisplayName("A valid refresh token is revoked by its jti and a new pair is issued")
        void shouldRotateRealRefreshToken() {
            String refreshToken = realJwtService.generateRefreshToken(activeUser);
            String jti = realJwtService.extractJti(refreshToken);
            when(tokenBlackListService.isTokenRevoked(jti)).thenReturn(false);
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

            AuthResponse result = realAuthService.refreshToken(refreshToken);

            verify(tokenBlackListService).revokeToken(jti, realJwtService.extractExpirationDateTime(refreshToken));
            assertNotEquals(refreshToken, result.getRefreshToken());
            assertTrue(realJwtService.isRefreshToken(result.getRefreshToken()));
            assertTrue(realJwtService.isAccessToken(result.getAccessToken()));
        }
    }

}
