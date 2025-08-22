package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.UserMapper;
import com.vaPaTi.vaPaTi.repository.RoleRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.UserService;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - deleteUser() Tests")
class UserServiceDeleteUserTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private UserService userService;

    private Long authenticatedUserId;
    private User activeUser;
    private User alreadyDeletedUser;

    @BeforeEach
    void setUp() {
        authenticatedUserId = 1L;

        activeUser = User.builder()
                .id(authenticatedUserId)
                .active(true)
                .verified(true)
                .deletedAt(null)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();

        alreadyDeletedUser = User.builder()
                .id(authenticatedUserId)
                .active(false)
                .verified(true)
                .deletedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();
    }

    @Test
    @DisplayName("Should successfully delete user when user exists and is not already deleted")
    void deleteUser_WhenUserExistsAndNotDeleted_ShouldDeleteUserSuccessfully() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        // When
        userService.deleteUser();

        // Then
        InOrder inOrder = inOrder(authenticatedUserService, userRepository);
        inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
        inOrder.verify(userRepository).findById(authenticatedUserId);
        inOrder.verify(userRepository).save(activeUser);

        assertNotNull(activeUser.getDeletedAt());
        assertTrue(activeUser.getDeletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(activeUser.getDeletedAt().isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    @Test
    @DisplayName("Should throw MessageException when authenticated user is not found in database")
    void deleteUser_WhenUserNotFound_ShouldThrowMessageException() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.empty());

        // When & Then
        MessageException exception = assertThrows(MessageException.class, () -> {
            userService.deleteUser();
        });

        assertEquals("User not found", exception.getMessage());

        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userRepository).findById(authenticatedUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw MessageException when user is already deleted")
    void deleteUser_WhenUserAlreadyDeleted_ShouldThrowMessageException() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(alreadyDeletedUser));

        // When & Then
        MessageException exception = assertThrows(MessageException.class, () -> {
            userService.deleteUser();
        });

        assertEquals("User is already deleted", exception.getMessage());

        InOrder inOrder = inOrder(authenticatedUserService, userRepository);
        inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
        inOrder.verify(userRepository).findById(authenticatedUserId);

        verify(userRepository, never()).save(any(User.class));
        assertEquals(alreadyDeletedUser.getDeletedAt(), alreadyDeletedUser.getDeletedAt());
    }

    @Test
    @DisplayName("Should verify exact interaction sequence for successful deletion")
    void deleteUser_SuccessfulDeletion_ShouldFollowExactInteractionSequence() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(activeUser);

        // When
        userService.deleteUser();

        // Then
        InOrder inOrder = inOrder(authenticatedUserService, userRepository);
        inOrder.verify(authenticatedUserService, times(1)).getAuthenticatedUserId();
        inOrder.verify(userRepository, times(1)).findById(authenticatedUserId);
        inOrder.verify(userRepository, times(1)).save(activeUser);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("Should set deletedAt to current timestamp within acceptable range")
    void deleteUser_WhenSuccessful_ShouldSetDeletedAtToCurrentTime() {
        // Given
        LocalDateTime beforeExecution = LocalDateTime.now();
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        // When
        userService.deleteUser();
        LocalDateTime afterExecution = LocalDateTime.now();

        // Then
        assertNotNull(activeUser.getDeletedAt());
        assertTrue(activeUser.getDeletedAt().isAfter(beforeExecution.minusSeconds(1)) ||
                activeUser.getDeletedAt().isEqual(beforeExecution.minusSeconds(1)));
        assertTrue(activeUser.getDeletedAt().isBefore(afterExecution.plusSeconds(1)) ||
                activeUser.getDeletedAt().isEqual(afterExecution.plusSeconds(1)));
    }

    @Test
    @DisplayName("Should handle edge case when user has deletedAt exactly at midnight")
    void deleteUser_WhenUserDeletedAtMidnight_ShouldThrowMessageException() {
        // Given
        User userDeletedAtMidnight = User.builder()
                .id(authenticatedUserId)
                .active(false)
                .deletedAt(LocalDateTime.of(2024, 1, 1, 0, 0, 0))
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(userDeletedAtMidnight));

        // When & Then
        MessageException exception = assertThrows(MessageException.class, () -> {
            userService.deleteUser();
        });

        assertEquals("User is already deleted", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should not modify any other user fields during deletion")
    void deleteUser_WhenSuccessful_ShouldOnlyModifyDeletedAtField() {
        // Given
        boolean originalActive = activeUser.isActive();
        boolean originalVerified = activeUser.isVerified();
        LocalDateTime originalCreatedAt = activeUser.getCreatedAt();
        LocalDateTime originalUpdatedAt = activeUser.getUpdatedAt();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        // When
        userService.deleteUser();

        // Then
        assertEquals(originalActive, activeUser.isActive());
        assertEquals(originalVerified, activeUser.isVerified());
        assertEquals(originalCreatedAt, activeUser.getCreatedAt());
        assertEquals(originalUpdatedAt, activeUser.getUpdatedAt());
        assertNotNull(activeUser.getDeletedAt()); // Only this field should be modified
    }

    @Test
    @DisplayName("Should verify repository save is called with the exact same user instance")
    void deleteUser_WhenSuccessful_ShouldSaveExactUserInstance() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(activeUser);

        // When
        userService.deleteUser();

        // Then
        verify(userRepository).save(activeUser); // Verify exact instance is saved
        verify(userRepository).save(argThat(user ->
                user.getId().equals(authenticatedUserId) &&
                        user.getDeletedAt() != null
        ));
    }

}
