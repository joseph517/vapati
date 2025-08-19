package com.vaPaTi.vaPaTi.service.Authentication;

import com.vaPaTi.vaPaTi.dtos.AuthResponse;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.service.AuthenticationService;
import com.vaPaTi.vaPaTi.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshToken Service Tests")
public class RefreshTokenServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthenticationService authService;

    // Test data
    private String validRefreshToken;
    private String userEmail;
    private String newAccessToken;
    private String newRefreshToken;
    private User activeUser;
    private User inactiveUser;
    private UserInfo userInfo;
    private Role userRole;

    @BeforeEach
    void setUp() {
        authService = new AuthenticationService(userRepository, jwtService, authenticationManager);

        // Setup test data
        validRefreshToken = "valid.refresh.token";
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

    @Test
    @DisplayName("Should successfully refresh token when all conditions are met")
    void shouldSuccessfullyRefreshTokenWhenAllConditionsAreMet() {
        // Given
        List<User> users = Arrays.asList(activeUser);

        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
        when(userRepository.findAllWithDetails()).thenReturn(users);
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

        // Verify method calls order
        InOrder inOrder = inOrder(jwtService, userRepository);
        inOrder.verify(jwtService).extractUsername(validRefreshToken);
        inOrder.verify(jwtService).isTokenValid(validRefreshToken, userEmail);
        inOrder.verify(userRepository).findAllWithDetails();
        inOrder.verify(jwtService).generateToken(activeUser);
        inOrder.verify(jwtService).generateRefreshToken(activeUser);

        // Verify all interactions
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verify(jwtService, times(1)).isTokenValid(validRefreshToken, userEmail);
        verify(userRepository, times(1)).findAllWithDetails();
        verify(jwtService, times(1)).generateToken(activeUser);
        verify(jwtService, times(1)).generateRefreshToken(activeUser);
        verifyNoMoreInteractions(jwtService, userRepository);
    }

    @Test
    @DisplayName("Should throw MessageException when refresh token is null")
    void shouldThrowMessageExceptionWhenRefreshTokenIsNull() {
        // Given
        String nullToken = null;

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(nullToken)
        );

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        // Verify no interactions with dependencies
        verifyNoInteractions(jwtService, userRepository, authenticationManager);
    }

    @Test
    @DisplayName("Should throw MessageException when refresh token is empty string")
    void shouldThrowMessageExceptionWhenRefreshTokenIsEmptyString() {
        // Given
        String emptyToken = "";

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(emptyToken)
        );

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        // Verify no interactions with dependencies
        verifyNoInteractions(jwtService, userRepository, authenticationManager);
    }

    @Test
    @DisplayName("Should throw MessageException when refresh token is only whitespace")
    void shouldThrowMessageExceptionWhenRefreshTokenIsOnlyWhitespace() {
        // Given
        String whitespaceToken = "   \t\n   ";

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(whitespaceToken)
        );

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        // Verify no interactions with dependencies
        verifyNoInteractions(jwtService, userRepository, authenticationManager);
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

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        // Verify interactions
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verify(jwtService, times(1)).isTokenValid(validRefreshToken, userEmail);
        verifyNoInteractions(userRepository);
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    @DisplayName("Should throw MessageException when user is not found")
    void shouldThrowMessageExceptionWhenUserIsNotFound() {
        // Given
        List<User> emptyUserList = Collections.emptyList();

        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
        when(userRepository.findAllWithDetails()).thenReturn(emptyUserList);

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals("User not found", exception.getMessage());

        // Verify interactions
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verify(jwtService, times(1)).isTokenValid(validRefreshToken, userEmail);
        verify(userRepository, times(1)).findAllWithDetails();
        verifyNoMoreInteractions(jwtService, userRepository);
    }

    @Test
    @DisplayName("Should throw MessageException when user email does not match (case insensitive)")
    void shouldThrowMessageExceptionWhenUserEmailDoesNotMatch() {
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

        List<User> users = Arrays.asList(differentUser);

        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
        when(userRepository.findAllWithDetails()).thenReturn(users);

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals("User not found", exception.getMessage());

        // Verify interactions
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verify(jwtService, times(1)).isTokenValid(validRefreshToken, userEmail);
        verify(userRepository, times(1)).findAllWithDetails();
        verifyNoMoreInteractions(jwtService, userRepository);
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

        List<User> users = Arrays.asList(upperCaseUser);
        String lowerCaseEmail = "user@example.com";

        when(jwtService.extractUsername(validRefreshToken)).thenReturn(lowerCaseEmail);
        when(jwtService.isTokenValid(validRefreshToken, lowerCaseEmail)).thenReturn(true);
        when(userRepository.findAllWithDetails()).thenReturn(users);
        when(jwtService.generateToken(upperCaseUser)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(upperCaseUser)).thenReturn(newRefreshToken);

        // When
        AuthResponse result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(newRefreshToken, result.getRefreshToken());
        assertEquals("USER@EXAMPLE.COM", result.getUserInfo().email);

        // Verify interactions
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verify(jwtService, times(1)).isTokenValid(validRefreshToken, lowerCaseEmail);
        verify(userRepository, times(1)).findAllWithDetails();
        verify(jwtService, times(1)).generateToken(upperCaseUser);
        verify(jwtService, times(1)).generateRefreshToken(upperCaseUser);
    }

    @Test
    @DisplayName("Should throw MessageException when user account is disabled")
    void shouldThrowMessageExceptionWhenUserAccountIsDisabled() {
        // Given
        List<User> users = Arrays.asList(inactiveUser);

        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
        when(userRepository.findAllWithDetails()).thenReturn(users);

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals("User account is disabled", exception.getMessage());

        // Verify interactions - should not generate new tokens
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verify(jwtService, times(1)).isTokenValid(validRefreshToken, userEmail);
        verify(userRepository, times(1)).findAllWithDetails();
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).generateRefreshToken(any(User.class));
        verifyNoMoreInteractions(jwtService, userRepository);
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

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        // Verify interactions
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verifyNoMoreInteractions(jwtService);
        verifyNoInteractions(userRepository);
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

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        // Verify interactions
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verify(jwtService, times(1)).isTokenValid(validRefreshToken, userEmail);
        verifyNoMoreInteractions(jwtService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw MessageException when user repository throws exception")
    void shouldThrowMessageExceptionWhenUserRepositoryThrowsException() {
        // Given
        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
        when(userRepository.findAllWithDetails())
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        // Verify interactions
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verify(jwtService, times(1)).isTokenValid(validRefreshToken, userEmail);
        verify(userRepository, times(1)).findAllWithDetails();
        verifyNoMoreInteractions(jwtService, userRepository);
    }

    @Test
    @DisplayName("Should throw MessageException when token generation throws exception")
    void shouldThrowMessageExceptionWhenTokenGenerationThrowsException() {
        // Given
        List<User> users = Arrays.asList(activeUser);

        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
        when(userRepository.findAllWithDetails()).thenReturn(users);
        when(jwtService.generateToken(activeUser))
                .thenThrow(new RuntimeException("Token generation error"));

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> authService.refreshToken(validRefreshToken)
        );

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        // Verify interactions
        verify(jwtService, times(1)).extractUsername(validRefreshToken);
        verify(jwtService, times(1)).isTokenValid(validRefreshToken, userEmail);
        verify(userRepository, times(1)).findAllWithDetails();
        verify(jwtService, times(1)).generateToken(activeUser);
        verify(jwtService, never()).generateRefreshToken(any(User.class));
        verifyNoMoreInteractions(jwtService, userRepository);
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

        List<User> users = Arrays.asList(otherUser, activeUser); // activeUser should be found

        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
        when(userRepository.findAllWithDetails()).thenReturn(users);
        when(jwtService.generateToken(activeUser)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(activeUser)).thenReturn(newRefreshToken);

        // When
        AuthResponse result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(newRefreshToken, result.getRefreshToken());
        assertEquals(activeUser.getId(), result.getUserInfo().userId);
        assertEquals(userEmail, result.getUserInfo().email);

        // Verify correct user was used for token generation
        verify(jwtService, times(1)).generateToken(activeUser);
        verify(jwtService, times(1)).generateRefreshToken(activeUser);
        verify(jwtService, never()).generateToken(otherUser);
        verify(jwtService, never()).generateRefreshToken(otherUser);
    }

    @Test
    @DisplayName("Should create UserInfo with correct full name concatenation")
    void shouldCreateUserInfoWithCorrectFullNameConcatenation() {
        // Given
        List<User> users = Arrays.asList(activeUser);

        when(jwtService.extractUsername(validRefreshToken)).thenReturn(userEmail);
        when(jwtService.isTokenValid(validRefreshToken, userEmail)).thenReturn(true);
        when(userRepository.findAllWithDetails()).thenReturn(users);
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

}
