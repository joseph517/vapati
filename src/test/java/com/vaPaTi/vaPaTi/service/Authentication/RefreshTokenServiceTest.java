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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
        authService = new AuthenticationService(userRepository, jwtService, authenticationManager, tokenBlackListService);

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

    // Stubs a signed, non-expired, non-revoked refresh token for the given email
    private void stubUsableRefreshToken(String email) {
        when(jwtService.extractUsername(validRefreshToken)).thenReturn(email);
        when(jwtService.isTokenValid(validRefreshToken, email)).thenReturn(true);
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
        stubUsableRefreshToken(userEmail);
        stubRotation();
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(activeUser));
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
        inOrder.verify(userRepository).findAllWithDetails();
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
        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(false);

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
        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
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
        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
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
        // Given - findAllWithDetails doesn't return deleted users
        stubUsableRefreshToken(userEmail);
        when(userRepository.findAllWithDetails()).thenReturn(Collections.emptyList());

        // When & Then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verify(tokenBlackListService, never()).revokeToken(any(), any());
    }

    @Test
    @DisplayName("Should respond 401 when user email does not match (case insensitive)")
    void shouldThrowInvalidCredentialsWhenUserEmailDoesNotMatch() {
        // Given
        UserInfo differentUserInfo = UserInfo.builder()
                .email("different@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .userName("janesmith")
                .build();

        User differentUser = User.builder()
                .id(3L)
                .userInfo(differentUserInfo)
                .role(userRole)
                .active(true)
                .build();

        stubUsableRefreshToken(userEmail);
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(differentUser));

        // When & Then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals(INVALID_REFRESH_TOKEN_MSG, exception.getMessage());
        verify(tokenBlackListService, never()).revokeToken(any(), any());
    }

    @Test
    @DisplayName("Should successfully find user with case insensitive email matching")
    void shouldSuccessfullyFindUserWithCaseInsensitiveEmailMatching() {
        // Given
        UserInfo upperCaseUserInfo = UserInfo.builder()
                .email("USER@EXAMPLE.COM")
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        User upperCaseUser = User.builder()
                .id(1L)
                .userInfo(upperCaseUserInfo)
                .role(userRole)
                .active(true)
                .build();

        String lowerCaseEmail = "user@example.com";
        stubUsableRefreshToken(lowerCaseEmail);
        stubRotation();
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(upperCaseUser));
        when(jwtService.generateToken(upperCaseUser)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(upperCaseUser)).thenReturn(newRefreshToken);

        // When
        AuthResponse result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(newRefreshToken, result.getRefreshToken());
        assertEquals("USER@EXAMPLE.COM", result.getUserInfo().email);
    }

    @Test
    @DisplayName("Should throw MessageException when user account is disabled")
    void shouldThrowMessageExceptionWhenUserAccountIsDisabled() {
        // Given
        stubUsableRefreshToken(userEmail);
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(inactiveUser));

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
        stubUsableRefreshToken(userEmail);
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(activeUser));

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
        stubUsableRefreshToken(userEmail);
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(activeUser));

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
        stubUsableRefreshToken(userEmail);
        stubRotation();
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(activeUser));
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
        when(jwtService.extractUsername(validRefreshToken))
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
        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail))
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
        stubUsableRefreshToken(userEmail);
        when(userRepository.findAllWithDetails())
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
        stubUsableRefreshToken(userEmail);
        stubRotation();
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(activeUser));
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
    @DisplayName("Should handle multiple users and find correct one by email")
    void shouldHandleMultipleUsersAndFindCorrectOneByEmail() {
        // Given
        UserInfo otherUserInfo = UserInfo.builder()
                .email("other@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .userName("janesmith")
                .build();

        User otherUser = User.builder()
                .id(2L)
                .userInfo(otherUserInfo)
                .role(userRole)
                .active(true)
                .build();

        stubUsableRefreshToken(userEmail);
        stubRotation();
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(otherUser, activeUser));
        when(jwtService.generateToken(activeUser)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(activeUser)).thenReturn(newRefreshToken);

        // When
        AuthResponse result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(activeUser.getId(), result.getUserInfo().userId);
        assertEquals(userEmail, result.getUserInfo().email);
        verify(jwtService, never()).generateToken(otherUser);
        verify(jwtService, never()).generateRefreshToken(otherUser);
    }

    @Test
    @DisplayName("Should create UserInfo with correct full name concatenation")
    void shouldCreateUserInfoWithCorrectFullNameConcatenation() {
        // Given
        stubUsableRefreshToken(userEmail);
        stubRotation();
        when(userRepository.findAllWithDetails()).thenReturn(Arrays.asList(activeUser));
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
            realAuthService = new AuthenticationService(userRepository, realJwtService, authenticationManager, tokenBlackListService);
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
        @DisplayName("A valid refresh token is revoked by its jti and a new pair is issued")
        void shouldRotateRealRefreshToken() {
            String refreshToken = realJwtService.generateRefreshToken(activeUser);
            String jti = realJwtService.extractJti(refreshToken);
            when(tokenBlackListService.isTokenRevoked(jti)).thenReturn(false);
            when(userRepository.findAllWithDetails()).thenReturn(List.of(activeUser));

            AuthResponse result = realAuthService.refreshToken(refreshToken);

            verify(tokenBlackListService).revokeToken(jti, realJwtService.extractExpirationDateTime(refreshToken));
            assertNotEquals(refreshToken, result.getRefreshToken());
            assertTrue(realJwtService.isRefreshToken(result.getRefreshToken()));
            assertTrue(realJwtService.isAccessToken(result.getAccessToken()));
        }
    }

}
