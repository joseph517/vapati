package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.UserMapper;
import com.vaPaTi.vaPaTi.repository.CampaignCategoryRepository;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.CampaignStatusHistoryRepository;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import com.vaPaTi.vaPaTi.repository.RoleRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.CampaignService;
import com.vaPaTi.vaPaTi.service.CampaignStatusHistoryService;
import com.vaPaTi.vaPaTi.service.UserService;
import com.vaPaTi.vaPaTi.validation.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.CampaignServiceValidation;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private CampaignService campaignService;

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
                .verified(true)
                .deletedAt(null)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();

        alreadyDeletedUser = User.builder()
                .id(authenticatedUserId)
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
        InOrder inOrder = inOrder(authenticatedUserService, userRepository, campaignService);
        inOrder.verify(authenticatedUserService, times(1)).getAuthenticatedUserId();
        inOrder.verify(userRepository, times(1)).findById(authenticatedUserId);
        inOrder.verify(campaignService, times(1)).closeAllByOwner(authenticatedUserId);
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
        boolean originalVerified = activeUser.isVerified();
        LocalDateTime originalCreatedAt = activeUser.getCreatedAt();
        LocalDateTime originalUpdatedAt = activeUser.getUpdatedAt();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        // When
        userService.deleteUser();

        // Then
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

    @Test
    @DisplayName("Should close the user's campaigns before marking the account as deleted")
    void deleteUser_WhenSuccessful_ShouldCloseCampaignsBeforeSettingDeletedAt() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(activeUser));
        doAnswer(invocation -> {
            assertNull(activeUser.getDeletedAt(), "campaigns must be closed while the owner is still active");
            return null;
        }).when(campaignService).closeAllByOwner(authenticatedUserId);

        // When
        userService.deleteUser();

        // Then
        InOrder inOrder = inOrder(campaignService, userRepository);
        inOrder.verify(campaignService).closeAllByOwner(authenticatedUserId);
        inOrder.verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("Should not close any campaign when the user is already deleted")
    void deleteUser_WhenUserAlreadyDeleted_ShouldNotCloseCampaigns() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(alreadyDeletedUser));

        // When & Then
        assertThrows(MessageException.class, () -> userService.deleteUser());
        verifyNoInteractions(campaignService);
    }

    @Test
    @DisplayName("Should not close any campaign when the user is not found")
    void deleteUser_WhenUserNotFound_ShouldNotCloseCampaigns() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(MessageException.class, () -> userService.deleteUser());
        verifyNoInteractions(campaignService);
    }

    @Nested
    @DisplayName("deleteUser() with a real CampaignService")
    class DeleteUserWithRealCampaignServiceTests {

        private CampaignRepository campaignRepository;
        private CampaignStatusHistoryService campaignStatusHistoryService;
        private UserService service;

        @BeforeEach
        void setUpRealCampaignService() {
            campaignRepository = mock(CampaignRepository.class);
            campaignStatusHistoryService = mock(CampaignStatusHistoryService.class);
            CampaignService realCampaignService = new CampaignService(
                    campaignRepository,
                    authenticatedUserService,
                    userRepository,
                    mock(CampaignServiceValidation.class),
                    mock(CampaignAuthorizationService.class),
                    mock(CampaignCategoryRepository.class),
                    mock(CategoryRepository.class),
                    campaignStatusHistoryService,
                    mock(CampaignStatusHistoryRepository.class)
            );
            service = new UserService(userRepository, userValidationService, userMapper, roleRepository,
                    authenticatedUserService, realCampaignService);

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
            when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(activeUser));
        }

        private Campaign campaignWithStatus(Long id, CampaignStatus status) {
            Goal goal = new Goal();
            goal.setStatus(status);
            Campaign campaign = new Campaign();
            campaign.setId(id);
            campaign.setUser(activeUser);
            campaign.setGoal(goal);
            return campaign;
        }

        @Test
        @DisplayName("Should close ACTIVE and COMPLETED campaigns, recording one transition each with the user as changedBy")
        void deleteUser_ShouldCloseActiveAndCompletedCampaignsRecordingTransitions() {
            // Given
            Campaign active = campaignWithStatus(10L, CampaignStatus.ACTIVE);
            Campaign completed = campaignWithStatus(11L, CampaignStatus.COMPLETED);
            when(campaignRepository.findByUserId(authenticatedUserId)).thenReturn(List.of(active, completed));

            // When
            service.deleteUser();

            // Then
            assertEquals(CampaignStatus.CLOSED, active.getGoal().getStatus());
            assertEquals(CampaignStatus.CLOSED, completed.getGoal().getStatus());
            verify(campaignStatusHistoryService)
                    .recordTransition(active, CampaignStatus.ACTIVE, CampaignStatus.CLOSED, authenticatedUserId);
            verify(campaignStatusHistoryService)
                    .recordTransition(completed, CampaignStatus.COMPLETED, CampaignStatus.CLOSED, authenticatedUserId);
            verify(campaignStatusHistoryService, times(2)).recordTransition(any(), any(), any(), any());
            verify(campaignRepository).saveAll(List.of(active, completed));
            verify(campaignRepository, never()).delete(any());
            assertNotNull(activeUser.getDeletedAt());
        }

        @Test
        @DisplayName("Should not record a transition nor change a campaign that was already CLOSED")
        void deleteUser_WithAlreadyClosedCampaign_ShouldNotRecordTransition() {
            // Given
            Campaign closed = campaignWithStatus(12L, CampaignStatus.CLOSED);
            Campaign active = campaignWithStatus(13L, CampaignStatus.ACTIVE);
            when(campaignRepository.findByUserId(authenticatedUserId)).thenReturn(List.of(closed, active));

            // When
            service.deleteUser();

            // Then
            assertEquals(CampaignStatus.CLOSED, closed.getGoal().getStatus());
            verify(campaignStatusHistoryService, never()).recordTransition(eq(closed), any(), any(), any());
            verify(campaignStatusHistoryService, times(1)).recordTransition(any(), any(), any(), any());
            verify(campaignRepository).saveAll(List.of(active));
        }

        @Test
        @DisplayName("Should delete the account without transitions when the user has no campaigns")
        void deleteUser_WithNoCampaigns_ShouldOnlyDeleteAccount() {
            // Given
            when(campaignRepository.findByUserId(authenticatedUserId)).thenReturn(List.of());

            // When
            service.deleteUser();

            // Then
            verifyNoInteractions(campaignStatusHistoryService);
            verify(userRepository).save(activeUser);
            assertNotNull(activeUser.getDeletedAt());
        }
    }
}
