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
import org.springframework.security.core.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        when(userRepository.findAllWithDetails())
                .thenReturn(List.of(mockUser));
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
        inOrder.verify(userRepository).findAllWithDetails();
        inOrder.verify(jwtService).generateToken(mockUser);
        inOrder.verify(jwtService).generateRefreshToken(mockUser);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when request is null")
    void authenticate_WithNullRequest_ShouldThrowIllegalArgumentException() {
        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");

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
        when(userRepository.findAllWithDetails())
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                .isInstanceOf(MessageException.class)
                .hasMessage("User not found");

        // Verify interactions
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findAllWithDetails();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should throw MessageException when user account is disabled")
    void authenticate_WithInactiveUser_ShouldThrowMessageException() {
        // Given
        User mockUser = mock(User.class);
        UserInfo mockUserInfo = mock(UserInfo.class);
        Authentication mockAuth = mock(Authentication.class);

        when(mockUser.isActive()).thenReturn(false);
        when(mockUser.getUserInfo()).thenReturn(mockUserInfo);
        when(mockUserInfo.getEmail()).thenReturn("test@example.com");

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findAllWithDetails())
                .thenReturn(List.of(mockUser));

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(validAuthRequest))
                .isInstanceOf(MessageException.class)
                .hasMessage("User account is disabled");

        // Verify interactions
        verify(authenticationManager).authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findAllWithDetails();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should handle case insensitive email matching")
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
        when(userRepository.findAllWithDetails())
                .thenReturn(List.of(mockUser));
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
        verify(userRepository).findAllWithDetails();
        verify(jwtService).generateToken(mockUser);
        verify(jwtService).generateRefreshToken(mockUser);
    }

    @Test
    @DisplayName("Should find correct user when multiple users exist")
    void authenticate_WithMultipleUsers_ShouldFindCorrectUser() {
        // Given
        User targetUser = mock(User.class);
        User otherUser = mock(User.class);
        UserInfo targetUserInfo = mock(UserInfo.class);
        UserInfo otherUserInfo = mock(UserInfo.class);
        Role mockRole = mock(Role.class);
        Authentication mockAuth = mock(Authentication.class);

        // Configure target user
        when(targetUser.isActive()).thenReturn(true);
        when(targetUser.getId()).thenReturn(1L);
        when(targetUser.getUserInfo()).thenReturn(targetUserInfo);
        when(targetUser.getRole()).thenReturn(mockRole);
        when(mockRole.getName()).thenReturn("USER");
        when(targetUserInfo.getEmail()).thenReturn("test@example.com");
        when(targetUserInfo.getFirstName()).thenReturn("John");
        when(targetUserInfo.getLastName()).thenReturn("Doe");
        when(targetUserInfo.getUserName()).thenReturn("johndoe");

        // Configure other user
        when(otherUser.getUserInfo()).thenReturn(otherUserInfo);
        when(otherUserInfo.getEmail()).thenReturn("other@example.com");

        when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findAllWithDetails())
                .thenReturn(Arrays.asList(otherUser, targetUser));
        when(jwtService.generateToken(targetUser))
                .thenReturn(expectedAccessToken);
        when(jwtService.generateRefreshToken(targetUser))
                .thenReturn(expectedRefreshToken);

        // When
        AuthResponse result = authenticationService.authenticate(validAuthRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserInfo().email).isEqualTo("test@example.com");

        verify(jwtService).generateToken(targetUser);
        verify(jwtService).generateRefreshToken(targetUser);
        verify(jwtService, never()).generateToken(otherUser);
        verify(jwtService, never()).generateRefreshToken(otherUser);
    }

}
