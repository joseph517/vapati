package com.vaPaTi.vaPaTi.service.Authentication;

import com.vaPaTi.vaPaTi.dtos.AuthRequest;
import com.vaPaTi.vaPaTi.dtos.AuthResponse;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.service.AuthenticationService;
import com.vaPaTi.vaPaTi.service.JwtService;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.InvalidCredentialsException;
import com.vaPaTi.vaPaTi.service.TokenBlackListService;
import com.vaPaTi.vaPaTi.validation.AccountStatusValidationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Nested;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService - authenticate method")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private Authentication authentication;
    @Mock
    private TokenBlackListService tokenBlackListService;
    @Spy
    private AccountStatusValidationService accountStatusValidationService = new AccountStatusValidationService();
    @InjectMocks
    private AuthenticationService authenticationService;

    private AuthRequest validAuthRequest;
    private String expectedAccessToken;
    private String expectedRefreshToken;

    @BeforeEach
    void setUp() {
        // Setup AuthRequest
        validAuthRequest = new AuthRequest("test@example.com", "password123");

        // Setup JWT tokens
        expectedAccessToken = "access.jwt.token";
        expectedRefreshToken = "refresh.jwt.token";
    }

    @Test
    @DisplayName("Should authenticate successfully with valid credentials and active user")
    void authenticate_WithValidCredentialsAndActiveUser_ShouldReturnAuthResponse() {
        // Given
        User mockUser = mock(User.class);
        UserInfo mockUserInfo = mock(UserInfo.class);
        Role mockRole = mock(Role.class);

        // Configure mocks behavior
        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getUserInfo()).thenReturn(mockUserInfo);
        when(mockUser.getRole()).thenReturn(mockRole);
        when(mockRole.getName()).thenReturn("USER");
        when(mockUserInfo.getEmail()).thenReturn("test@example.com");
        when(mockUserInfo.getFirstName()).thenReturn("John");
        when(mockUserInfo.getLastName()).thenReturn("Doe");
        when(mockUserInfo.getUserName()).thenReturn("johndoe");

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser))
                .thenReturn(expectedAccessToken);
        when(jwtService.generateRefreshToken(mockUser))
                .thenReturn(expectedRefreshToken);

        // When
        AuthResponse result = authenticationService.authenticate(validAuthRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(expectedAccessToken);
        assertThat(result.getRefreshToken()).isEqualTo(expectedRefreshToken);

        AuthResponse.UserInfo userInfo = result.getUserInfo();
        assertThat(userInfo).isNotNull();
        assertThat(result.getUserInfo().email).isEqualTo("test@example.com");
        assertThat(result.getUserInfo().role).isEqualTo("USER");
        assertThat(result.getUserInfo().firstName).isEqualTo("John");
        assertThat(result.getUserInfo().lastName).isEqualTo("Doe");
        assertThat(result.getUserInfo().userName).isEqualTo("johndoe");
        assertThat(result.getUserInfo().fullName).isEqualTo("John Doe");

        // Verify interactions in order
        InOrder inOrder = inOrder(authenticationManager, userRepository, jwtService);
        inOrder.verify(authenticationManager).authenticate(ArgumentMatchers.argThat(auth ->
                auth instanceof UsernamePasswordAuthenticationToken &&
                        auth.getPrincipal().equals("test@example.com") &&
                        auth.getCredentials().equals("password123")
        ));
        inOrder.verify(userRepository).findByEmailIncludingDeleted("test@example.com");
        inOrder.verify(jwtService).generateToken(mockUser);
        inOrder.verify(jwtService).generateRefreshToken(mockUser);
    }

    @Test
    @DisplayName("Should throw MessageException when request is null")
    void authenticate_WithNullRequest_ShouldThrowMessageException() {
        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(null))
                .isInstanceOf(MessageException.class)
                .hasMessage("Authentication request cannot be null");
        // Verify no interactions with dependencies
        verifyNoInteractions(authenticationManager, userRepository, jwtService);
    }

    @Test
    @DisplayName("Should throw MessageException when credentials are invalid")
    void authenticate_WithInvalidCredentials_ShouldThrowMessageException() {
        // Given
        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                .isInstanceOf(MessageException.class)
                .hasMessage("Invalid email or password");

        // Verify authentication was attempted but other services were not called
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    @DisplayName("Should throw MessageException when user is not found")
    void authenticate_WithUserNotFound_ShouldThrowMessageException() {
        // Given
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                .isInstanceOf(MessageException.class)
                .hasMessage("User not found");

        // Verify interactions
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailIncludingDeleted("test@example.com");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should throw MessageException when user account is disabled")
    void authenticate_WithInactiveUser_ShouldThrowMessageException() {
        // Given
        User mockUser = mock(User.class);
        Authentication mockAuth = mock(Authentication.class);

        when(mockUser.isActive()).thenReturn(false);

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                .thenReturn(Optional.of(mockUser));

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                .isInstanceOf(MessageException.class)
                .hasMessage("User account is disabled");

        // Verify interactions
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailIncludingDeleted("test@example.com");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should look up the user with the email as received (the DB collation is case insensitive)")
    void authenticate_WithCaseInsensitiveEmail_ShouldAuthenticateSuccessfully() {
        // Given
        AuthRequest upperCaseEmailRequest = new AuthRequest("TEST@EXAMPLE.COM", "password123");
        User mockUser = mock(User.class);
        UserInfo mockUserInfo = mock(UserInfo.class);
        Role mockRole = mock(Role.class);
        Authentication mockAuth = mock(Authentication.class);

        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getUserInfo()).thenReturn(mockUserInfo);
        when(mockUser.getRole()).thenReturn(mockRole);
        when(mockRole.getName()).thenReturn("USER");
        when(mockUserInfo.getEmail()).thenReturn("test@example.com");
        when(mockUserInfo.getFirstName()).thenReturn("John");
        when(mockUserInfo.getLastName()).thenReturn("Doe");
        when(mockUserInfo.getUserName()).thenReturn("johndoe");

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByEmailIncludingDeleted("TEST@EXAMPLE.COM"))
                .thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser))
                .thenReturn(expectedAccessToken);
        when(jwtService.generateRefreshToken(mockUser))
                .thenReturn(expectedRefreshToken);

        // When
        AuthResponse result = authenticationService.authenticate(upperCaseEmailRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(expectedAccessToken);
        assertThat(result.getRefreshToken()).isEqualTo(expectedRefreshToken);

        verify(authenticationManager).authenticate(ArgumentMatchers.argThat(auth ->
                auth.getPrincipal().equals("TEST@EXAMPLE.COM")
        ));
        verify(userRepository).findByEmailIncludingDeleted("TEST@EXAMPLE.COM");
        verify(jwtService).generateToken(mockUser);
        verify(jwtService).generateRefreshToken(mockUser);
    }

    @Test
    @DisplayName("Should throw MessageException when user is banned with custom reason")
    void authenticate_WithBannedUserWithReason_ShouldThrowMessageException() {
        // Given
        User mockUser = mock(User.class);
        Authentication mockAuth = mock(Authentication.class);

        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getBanned()).thenReturn(true);
        when(mockUser.getBannedReason()).thenReturn("Custom ban reason");

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                .thenReturn(Optional.of(mockUser));

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                .isInstanceOf(MessageException.class)
                .hasMessage("Your account has been banned. Reason: Custom ban reason");

        // Verify interactions
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailIncludingDeleted("test@example.com");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should throw MessageException when user is banned without reason")
    void authenticate_WithBannedUserWithoutReason_ShouldThrowMessageException() {
        // Given
        User mockUser = mock(User.class);
        Authentication mockAuth = mock(Authentication.class);

        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getBanned()).thenReturn(true);
        when(mockUser.getBannedReason()).thenReturn(null);

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                .thenReturn(Optional.of(mockUser));

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                .isInstanceOf(MessageException.class)
                .hasMessage("Your account has been banned. Reason: Violation of terms");

        // Verify interactions
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailIncludingDeleted("test@example.com");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should throw MessageException when user is suspended with custom reason")
    void authenticate_WithSuspendedUserWithReason_ShouldThrowMessageException() {
        // Given
        User mockUser = mock(User.class);
        Authentication mockAuth = mock(Authentication.class);
        LocalDateTime suspensionEnd = LocalDateTime.now().plusDays(7);

        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getBanned()).thenReturn(null);
        when(mockUser.getSuspendedUntil()).thenReturn(suspensionEnd);
        when(mockUser.getBannedReason()).thenReturn("Custom suspension reason");

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                .thenReturn(Optional.of(mockUser));

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("Your account is suspended until " + suspensionEnd)
                .hasMessageContaining("Custom suspension reason");

        // Verify interactions
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailIncludingDeleted("test@example.com");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should throw MessageException when user is suspended without reason")
    void authenticate_WithSuspendedUserWithoutReason_ShouldThrowMessageException() {
        // Given
        User mockUser = mock(User.class);
        Authentication mockAuth = mock(Authentication.class);
        LocalDateTime suspensionEnd = LocalDateTime.now().plusDays(7);

        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getBanned()).thenReturn(null);
        when(mockUser.getSuspendedUntil()).thenReturn(suspensionEnd);
        when(mockUser.getBannedReason()).thenReturn(null);

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                .thenReturn(Optional.of(mockUser));

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("Your account is suspended until " + suspensionEnd)
                .hasMessageContaining("Violation of terms");

        // Verify interactions
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailIncludingDeleted("test@example.com");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should authenticate successfully when suspension has expired")
    void authenticate_WithExpiredSuspension_ShouldAuthenticateSuccessfully() {
        // Given
        User mockUser = mock(User.class);
        UserInfo mockUserInfo = mock(UserInfo.class);
        Role mockRole = mock(Role.class);
        Authentication mockAuth = mock(Authentication.class);
        LocalDateTime expiredSuspension = LocalDateTime.now().minusDays(1);

        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getBanned()).thenReturn(null);
        when(mockUser.getSuspendedUntil()).thenReturn(expiredSuspension);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getUserInfo()).thenReturn(mockUserInfo);
        when(mockUser.getRole()).thenReturn(mockRole);
        when(mockRole.getName()).thenReturn("USER");
        when(mockUserInfo.getEmail()).thenReturn("test@example.com");
        when(mockUserInfo.getFirstName()).thenReturn("John");
        when(mockUserInfo.getLastName()).thenReturn("Doe");
        when(mockUserInfo.getUserName()).thenReturn("johndoe");

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser))
                .thenReturn(expectedAccessToken);
        when(jwtService.generateRefreshToken(mockUser))
                .thenReturn(expectedRefreshToken);

        // When
        AuthResponse result = authenticationService.authenticate(validAuthRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(expectedAccessToken);
        assertThat(result.getRefreshToken()).isEqualTo(expectedRefreshToken);

        // Verify interactions
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailIncludingDeleted("test@example.com");
        verify(jwtService).generateToken(mockUser);
        verify(jwtService).generateRefreshToken(mockUser);
    }

    @Nested
    @DisplayName("authenticate() - deleted account restoration")
    class RestoreDeletedAccountTests {

        private User deletedUser;
        private LocalDateTime deletedAt;

        @BeforeEach
        void setUpDeletedUser() {
            deletedAt = LocalDateTime.now().minusDays(2);
            UserInfo info = UserInfo.builder()
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .userName("johndoe")
                    .build();
            deletedUser = User.builder()
                    .id(1L)
                    .userInfo(info)
                    .role(Role.builder().id(1L).name("USER").build())
                    .active(true)
                    .build();
            deletedUser.setDeletedAt(deletedAt);
            info.setUser(deletedUser);

            when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                    .thenReturn(Optional.of(deletedUser));
        }

        @Test
        @DisplayName("Deleted account with correct password: restored and tokens issued")
        void shouldRestoreDeletedAccount() {
            when(jwtService.generateToken(deletedUser)).thenReturn(expectedAccessToken);
            when(jwtService.generateRefreshToken(deletedUser)).thenReturn(expectedRefreshToken);

            AuthResponse result = authenticationService.authenticate(validAuthRequest);

            assertThat(result.getAccessToken()).isEqualTo(expectedAccessToken);
            assertThat(deletedUser.getDeletedAt()).isNull();

            // Restores only after validating the password and the account status
            InOrder inOrder = inOrder(authenticationManager, accountStatusValidationService, userRepository, jwtService);
            inOrder.verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
            inOrder.verify(accountStatusValidationService).validateNotBlocked(deletedUser);
            inOrder.verify(userRepository).save(deletedUser);
            inOrder.verify(jwtService).generateToken(deletedUser);
        }

        @Test
        @DisplayName("Deleted and banned account: 403 with the reason, not restored")
        void shouldNotRestoreDeletedAndBannedAccount() {
            deletedUser.setBanned(true);
            deletedUser.setBannedReason("Spam");

            assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Your account has been banned. Reason: Spam");

            assertThat(deletedUser.getDeletedAt()).isEqualTo(deletedAt);
            verify(userRepository, never()).save(ArgumentMatchers.any());
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Deleted and suspended account: 403 with the reason, not restored")
        void shouldNotRestoreDeletedAndSuspendedAccount() {
            deletedUser.setSuspendedUntil(LocalDateTime.now().plusDays(5));

            assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessageStartingWith("Your account is suspended until");

            assertThat(deletedUser.getDeletedAt()).isEqualTo(deletedAt);
            verify(userRepository, never()).save(ArgumentMatchers.any());
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Deleted and disabled account: 403, not restored")
        void shouldNotRestoreDeletedAndDisabledAccount() {
            deletedUser.setActive(false);

            assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("User account is disabled");

            assertThat(deletedUser.getDeletedAt()).isEqualTo(deletedAt);
            verify(userRepository, never()).save(ArgumentMatchers.any());
        }

        @Test
        @DisplayName("Account that is not deleted: nothing is saved")
        void shouldNotSaveWhenAccountIsNotDeleted() {
            deletedUser.setDeletedAt(null);
            when(jwtService.generateToken(deletedUser)).thenReturn(expectedAccessToken);
            when(jwtService.generateRefreshToken(deletedUser)).thenReturn(expectedRefreshToken);

            authenticationService.authenticate(validAuthRequest);

            verify(userRepository, never()).save(ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        private static final String TEST_SECRET = "mySecretKeyForTestingThatIsLongEnoughForHS256Algorithm";
        private static final String INVALID_REFRESH_TOKEN_MSG = "Invalid or expired refresh token";

        private JwtService realJwtService;
        private AuthenticationService logoutService;
        private User user;
        private User otherUser;

        @BeforeEach
        void setUpLogout() {
            realJwtService = new JwtService();
            ReflectionTestUtils.setField(realJwtService, "jwtSecret", TEST_SECRET);
            ReflectionTestUtils.setField(realJwtService, "jwtExpirationMs", 3600000L);
            ReflectionTestUtils.setField(realJwtService, "jwtRefreshExpirationMs", 604800000L);
            realJwtService.init();
            logoutService = new AuthenticationService(userRepository, realJwtService, authenticationManager, tokenBlackListService, new AccountStatusValidationService());

            user = buildUser(1L, "test@example.com");
            otherUser = buildUser(2L, "other@example.com");
        }

        private User buildUser(Long id, String email) {
            UserInfo info = UserInfo.builder()
                    .email(email)
                    .firstName("John")
                    .lastName("Doe")
                    .userName("user" + id)
                    .build();
            User u = User.builder()
                    .id(id)
                    .userInfo(info)
                    .role(Role.builder().id(1L).name("USER").build())
                    .active(true)
                    .build();
            info.setUser(u);
            return u;
        }

        private String signedToken(String secret, String type, Date expiration) {
            var builder = Jwts.builder()
                    .setSubject("test@example.com")
                    .setId(UUID.randomUUID().toString())
                    .claim("userId", 1L)
                    .setIssuedAt(new Date(expiration.getTime() - 3600000))
                    .setExpiration(expiration)
                    .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256);
            if (type != null) {
                builder.claim("type", type);
            }
            return builder.compact();
        }

        private void assertInvalidRefresh(String refreshToken) {
            assertThatThrownBy(() -> logoutService.logout(refreshToken, null))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage(INVALID_REFRESH_TOKEN_MSG);
            verify(tokenBlackListService, never()).revokeToken(any(), any());
        }

        @Test
        @DisplayName("Valid refresh and access of the same user: revokes both jtis")
        void shouldRevokeRefreshAndAccessOfSameUser() {
            String refreshToken = realJwtService.generateRefreshToken(user);
            String accessToken = realJwtService.generateToken(user);
            when(tokenBlackListService.isTokenRevoked(realJwtService.extractJti(refreshToken))).thenReturn(false);

            logoutService.logout(refreshToken, accessToken);

            verify(tokenBlackListService).revokeToken(
                    realJwtService.extractJti(refreshToken), realJwtService.extractExpirationDateTime(refreshToken));
            verify(tokenBlackListService).revokeToken(
                    realJwtService.extractJti(accessToken), realJwtService.extractExpirationDateTime(accessToken));
        }

        @Test
        @DisplayName("Without access token: revokes only the refresh")
        void shouldRevokeOnlyRefreshWithoutAccessToken() {
            String refreshToken = realJwtService.generateRefreshToken(user);
            when(tokenBlackListService.isTokenRevoked(realJwtService.extractJti(refreshToken))).thenReturn(false);

            logoutService.logout(refreshToken, null);

            verify(tokenBlackListService, times(1)).revokeToken(any(), any());
            verify(tokenBlackListService).revokeToken(eq(realJwtService.extractJti(refreshToken)), any());
        }

        @Test
        @DisplayName("Refresh already revoked: does nothing (idempotent)")
        void shouldDoNothingWhenRefreshAlreadyRevoked() {
            String refreshToken = realJwtService.generateRefreshToken(user);
            String accessToken = realJwtService.generateToken(user);
            when(tokenBlackListService.isTokenRevoked(realJwtService.extractJti(refreshToken))).thenReturn(true);

            logoutService.logout(refreshToken, accessToken);

            verify(tokenBlackListService, never()).revokeToken(any(), any());
        }

        @Test
        @DisplayName("Expired refresh: does nothing (idempotent)")
        void shouldDoNothingWhenRefreshExpired() {
            String expiredRefresh = signedToken(TEST_SECRET, JwtService.REFRESH_TOKEN_TYPE,
                    new Date(System.currentTimeMillis() - 60000));

            logoutService.logout(expiredRefresh, null);

            verifyNoInteractions(tokenBlackListService);
        }

        @Test
        @DisplayName("Concurrent logout revoking the same jti: does not fail")
        void shouldNotFailOnConcurrentRevocation() {
            String refreshToken = realJwtService.generateRefreshToken(user);
            when(tokenBlackListService.isTokenRevoked(realJwtService.extractJti(refreshToken))).thenReturn(false);
            doThrow(new DataIntegrityViolationException("Violation of UNIQUE KEY constraint"))
                    .when(tokenBlackListService).revokeToken(any(), any());

            logoutService.logout(refreshToken, null);

            verify(tokenBlackListService).revokeToken(any(), any());
        }

        @Test
        @DisplayName("Null or blank refresh: 401")
        void shouldRejectBlankRefresh() {
            assertInvalidRefresh(null);
            assertInvalidRefresh("");
            assertInvalidRefresh("   ");
            verifyNoInteractions(tokenBlackListService);
        }

        @Test
        @DisplayName("Malformed refresh: 401")
        void shouldRejectMalformedRefresh() {
            assertInvalidRefresh("basura");
            verifyNoInteractions(tokenBlackListService);
        }

        @Test
        @DisplayName("Refresh signed with another secret: 401")
        void shouldRejectRefreshWithInvalidSignature() {
            String forged = signedToken("anotherSecretKeyForTestingThatIsLongEnoughForHS256", JwtService.REFRESH_TOKEN_TYPE,
                    new Date(System.currentTimeMillis() + 3600000));

            assertInvalidRefresh(forged);
            verifyNoInteractions(tokenBlackListService);
        }

        @Test
        @DisplayName("Access token sent as refresh: 401")
        void shouldRejectAccessTokenAsRefresh() {
            assertInvalidRefresh(realJwtService.generateToken(user));
            verifyNoInteractions(tokenBlackListService);
        }

        @Test
        @DisplayName("Token without type sent as refresh: 401")
        void shouldRejectTokenWithoutTypeAsRefresh() {
            assertInvalidRefresh(signedToken(TEST_SECRET, null, new Date(System.currentTimeMillis() + 3600000)));
            verifyNoInteractions(tokenBlackListService);
        }

        @Test
        @DisplayName("Invalid access in header: ignored, refresh still revoked")
        void shouldIgnoreInvalidAccessToken() {
            String refreshToken = realJwtService.generateRefreshToken(user);
            when(tokenBlackListService.isTokenRevoked(realJwtService.extractJti(refreshToken))).thenReturn(false);

            logoutService.logout(refreshToken, "invalid.jwt.token");

            verify(tokenBlackListService, times(1)).revokeToken(eq(realJwtService.extractJti(refreshToken)), any());
        }

        @Test
        @DisplayName("Access of another user in header: ignored")
        void shouldIgnoreAccessTokenOfAnotherUser() {
            String refreshToken = realJwtService.generateRefreshToken(user);
            String foreignAccess = realJwtService.generateToken(otherUser);
            when(tokenBlackListService.isTokenRevoked(realJwtService.extractJti(refreshToken))).thenReturn(false);

            logoutService.logout(refreshToken, foreignAccess);

            verify(tokenBlackListService, times(1)).revokeToken(any(), any());
            verify(tokenBlackListService, never()).revokeToken(eq(realJwtService.extractJti(foreignAccess)), any());
        }

        @Test
        @DisplayName("Refresh token in the Authorization header: ignored")
        void shouldIgnoreRefreshTypeTokenInHeader() {
            String refreshToken = realJwtService.generateRefreshToken(user);
            String otherRefresh = realJwtService.generateRefreshToken(user);
            when(tokenBlackListService.isTokenRevoked(realJwtService.extractJti(refreshToken))).thenReturn(false);

            logoutService.logout(refreshToken, otherRefresh);

            verify(tokenBlackListService, times(1)).revokeToken(any(), any());
            verify(tokenBlackListService, never()).revokeToken(eq(realJwtService.extractJti(otherRefresh)), any());
        }
    }
}
