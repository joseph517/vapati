package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignAuthorizationService - Unit Tests")
class CampaignAuthorizationServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CampaignAuthorizationService campaignAuthorizationService;

    private Campaign testCampaign;
    private User testOwner;
    private User testAdmin;
    private User testRegularUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder()
                .id(1L)
                .name("ADMIN")
                .build();

        userRole = Role.builder()
                .id(2L)
                .name("USER")
                .build();

        testOwner = User.builder()
                .id(1L)
                .role(userRole)
                .build();

        testAdmin = User.builder()
                .id(2L)
                .role(adminRole)
                .build();

        testRegularUser = User.builder()
                .id(3L)
                .role(userRole)
                .build();

        testCampaign = Campaign.builder()
                .id(1L)
                .name("Test Campaign")
                .description("Test Description")
                .user(testOwner)
                .build();
    }

    @Nested
    @DisplayName("validateOwnershipOrAdmin Tests")
    class ValidateOwnershipOrAdminTests {

        @Test
        @DisplayName("Should validate successfully when user is the campaign owner")
        void validateOwnershipOrAdmin_WhenUserIsOwner_ShouldNotThrowException() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));

            // When & Then (should not throw exception)
            campaignAuthorizationService.validateOwnershipOrAdmin(1L, 1L);

            verify(campaignRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should validate successfully when user is admin")
        void validateOwnershipOrAdmin_WhenUserIsAdmin_ShouldNotThrowException() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(2L)).thenReturn(Optional.of(testAdmin));

            // When & Then (should not throw exception)
            campaignAuthorizationService.validateOwnershipOrAdmin(1L, 2L);

            verify(campaignRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findById(2L);
        }

        @Test
        @DisplayName("Should throw MessageException when user is neither owner nor admin")
        void validateOwnershipOrAdmin_WhenUserIsNeitherOwnerNorAdmin_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(3L)).thenReturn(Optional.of(testRegularUser));

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(1L, 3L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You are not authorized to perform this action");

            verify(campaignRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findById(3L);
        }

        @Test
        @DisplayName("Should throw MessageException when campaign not found")
        void validateOwnershipOrAdmin_WhenCampaignNotFound_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(999L, 1L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign not found with id: 999");

            verify(campaignRepository, times(1)).findById(999L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw MessageException when user not found")
        void validateOwnershipOrAdmin_WhenUserNotFound_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(1L, 999L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User not found with id: 999");

            verify(campaignRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findById(999L);
        }

        @Test
        @DisplayName("Should handle user with null role gracefully")
        void validateOwnershipOrAdmin_WhenUserHasNullRole_ShouldCheckOwnershipOnly() {
            // Given
            User userWithNullRole = User.builder()
                    .id(4L)
                    .role(null)
                    .build();

            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(4L)).thenReturn(Optional.of(userWithNullRole));

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(1L, 4L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You are not authorized to perform this action");

            verify(campaignRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findById(4L);
        }
    }

    @Nested
    @DisplayName("canCloseCampaign Tests")
    class CanCloseCampaignTests {

        @Test
        @DisplayName("Should return true when user is the campaign owner")
        void canCloseCampaign_WhenUserIsOwner_ShouldReturnTrue() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));

            // When
            boolean result = campaignAuthorizationService.canCloseCampaign(1L, 1L);

            // Then
            assertThat(result).isTrue();

            verify(campaignRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should return true when user is admin")
        void canCloseCampaign_WhenUserIsAdmin_ShouldReturnTrue() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(2L)).thenReturn(Optional.of(testAdmin));

            // When
            boolean result = campaignAuthorizationService.canCloseCampaign(1L, 2L);

            // Then
            assertThat(result).isTrue();

            verify(campaignRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findById(2L);
        }

        @Test
        @DisplayName("Should return false when user is neither owner nor admin")
        void canCloseCampaign_WhenUserIsUnauthorized_ShouldReturnFalse() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(3L)).thenReturn(Optional.of(testRegularUser));

            // When
            boolean result = campaignAuthorizationService.canCloseCampaign(1L, 3L);

            // Then
            assertThat(result).isFalse();

            verify(campaignRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findById(3L);
        }

        @Test
        @DisplayName("Should return false when campaign not found")
        void canCloseCampaign_WhenCampaignNotFound_ShouldReturnFalse() {
            // Given
            when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = campaignAuthorizationService.canCloseCampaign(999L, 1L);

            // Then
            assertThat(result).isFalse();

            verify(campaignRepository, times(1)).findById(999L);
        }
    }

    @Nested
    @DisplayName("getCampaignIfAuthorized Tests")
    class GetCampaignIfAuthorizedTests {

        @Test
        @DisplayName("Should return campaign when user is the campaign owner")
        void getCampaignIfAuthorized_WhenUserIsOwner_ShouldReturnCampaign() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));

            // When
            Campaign result = campaignAuthorizationService.getCampaignIfAuthorized(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test Campaign");

            verify(campaignRepository, times(2)).findById(1L); // Called once in validateOwnershipOrAdmin, once in getCampaignIfAuthorized
            verify(userRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should return campaign when user is admin")
        void getCampaignIfAuthorized_WhenUserIsAdmin_ShouldReturnCampaign() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(2L)).thenReturn(Optional.of(testAdmin));

            // When
            Campaign result = campaignAuthorizationService.getCampaignIfAuthorized(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test Campaign");

            verify(campaignRepository, times(2)).findById(1L); // Called once in validateOwnershipOrAdmin, once in getCampaignIfAuthorized
            verify(userRepository, times(1)).findById(2L);
        }

        @Test
        @DisplayName("Should throw MessageException when user is unauthorized")
        void getCampaignIfAuthorized_WhenUserIsUnauthorized_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findById(3L)).thenReturn(Optional.of(testRegularUser));

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.getCampaignIfAuthorized(1L, 3L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You are not authorized to perform this action");

            verify(campaignRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findById(3L);
        }

        @Test
        @DisplayName("Should throw MessageException when campaign not found during validation")
        void getCampaignIfAuthorized_WhenCampaignNotFoundDuringValidation_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.getCampaignIfAuthorized(999L, 1L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign not found with id: 999");

            verify(campaignRepository, times(1)).findById(999L);
            verify(userRepository, never()).findById(any());
        }
    }
}
