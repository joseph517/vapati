package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(1L)).thenReturn(Optional.of(testOwner));

            // When & Then (should not throw exception)
            campaignAuthorizationService.validateOwnershipOrAdmin(1L, 1L);

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(1L);
            verify(userRepository, times(1)).findByIdForRequest(1L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should validate successfully when user is admin")
        void validateOwnershipOrAdmin_WhenUserIsAdmin_ShouldNotThrowException() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(2L)).thenReturn(Optional.of(testAdmin));

            // When & Then (should not throw exception)
            campaignAuthorizationService.validateOwnershipOrAdmin(1L, 2L);

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(1L);
            verify(userRepository, times(1)).findByIdForRequest(2L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw MessageException when user is neither owner nor admin")
        void validateOwnershipOrAdmin_WhenUserIsNeitherOwnerNorAdmin_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(3L)).thenReturn(Optional.of(testRegularUser));

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(1L, 3L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You are not authorized to perform this action");

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(1L);
            verify(userRepository, times(1)).findByIdForRequest(3L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw MessageException when campaign not found")
        void validateOwnershipOrAdmin_WhenCampaignNotFound_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(999L, 1L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign not found with id: 999");

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(999L);
            verify(userRepository, never()).findByIdForRequest(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the campaign owner is deleted")
        void validateOwnershipOrAdmin_WhenOwnerIsDeleted_ShouldThrowResourceNotFoundException() {
            // Given: the owner filter excludes the campaign, even though findById would still return it
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Campaign not found with id: 1");

            verify(campaignRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw MessageException when user not found")
        void validateOwnershipOrAdmin_WhenUserNotFound_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(1L, 999L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User not found with id: 999");

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(1L);
            verify(userRepository, times(1)).findByIdForRequest(999L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should handle user with null role gracefully")
        void validateOwnershipOrAdmin_WhenUserHasNullRole_ShouldCheckOwnershipOnly() {
            // Given
            User userWithNullRole = User.builder()
                    .id(4L)
                    .role(null)
                    .build();

            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(4L)).thenReturn(Optional.of(userWithNullRole));

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(1L, 4L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You are not authorized to perform this action");

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(1L);
            verify(userRepository, times(1)).findByIdForRequest(4L);
            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("validateOwnershipOrAdmin - campaign status Tests")
    class ValidateOwnershipOrAdminStatusTests {

        private void givenCampaignWithStatus(CampaignStatus status) {
            testCampaign.setGoal(Goal.builder().id(1L).status(status).build());
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when a third party targets a CLOSED campaign")
        void validateOwnershipOrAdmin_WhenThirdPartyAndCampaignClosed_ShouldThrowResourceNotFoundException() {
            // Given
            givenCampaignWithStatus(CampaignStatus.CLOSED);
            when(userRepository.findByIdForRequest(3L)).thenReturn(Optional.of(testRegularUser));

            // When & Then: same message as a missing campaign, so the CLOSED campaign is not revealed
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(1L, 3L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Campaign not found with id: 1");
        }

        @ParameterizedTest
        @EnumSource(value = CampaignStatus.class, names = {"ACTIVE", "COMPLETED"})
        @DisplayName("Should throw ForbiddenActionException when a third party targets a non-CLOSED campaign")
        void validateOwnershipOrAdmin_WhenThirdPartyAndCampaignNotClosed_ShouldThrowForbiddenActionException(CampaignStatus status) {
            // Given
            givenCampaignWithStatus(status);
            when(userRepository.findByIdForRequest(3L)).thenReturn(Optional.of(testRegularUser));

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.validateOwnershipOrAdmin(1L, 3L))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("You are not authorized to perform this action");
        }

        @ParameterizedTest
        @EnumSource(CampaignStatus.class)
        @DisplayName("Should validate successfully when the owner targets a campaign in any status")
        void validateOwnershipOrAdmin_WhenOwner_ShouldNotThrowForAnyStatus(CampaignStatus status) {
            // Given
            givenCampaignWithStatus(status);
            when(userRepository.findByIdForRequest(1L)).thenReturn(Optional.of(testOwner));

            // When & Then (should not throw exception)
            campaignAuthorizationService.validateOwnershipOrAdmin(1L, 1L);
        }

        @ParameterizedTest
        @EnumSource(CampaignStatus.class)
        @DisplayName("Should validate successfully when an admin targets a campaign in any status")
        void validateOwnershipOrAdmin_WhenAdmin_ShouldNotThrowForAnyStatus(CampaignStatus status) {
            // Given
            givenCampaignWithStatus(status);
            when(userRepository.findByIdForRequest(2L)).thenReturn(Optional.of(testAdmin));

            // When & Then (should not throw exception)
            campaignAuthorizationService.validateOwnershipOrAdmin(1L, 2L);
        }
    }

    @Nested
    @DisplayName("isAdmin Tests")
    class IsAdminTests {

        @Test
        @DisplayName("Should return true when the user has the ADMIN role")
        void isAdmin_WhenUserIsAdmin_ShouldReturnTrue() {
            when(userRepository.existsByIdAndRole_Name(2L, "ADMIN")).thenReturn(true);

            assertThat(campaignAuthorizationService.isAdmin(2L)).isTrue();
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return false when the user has another role")
        void isAdmin_WhenUserIsNotAdmin_ShouldReturnFalse() {
            when(userRepository.existsByIdAndRole_Name(3L, "ADMIN")).thenReturn(false);

            assertThat(campaignAuthorizationService.isAdmin(3L)).isFalse();
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return false when the user has no role")
        void isAdmin_WhenUserHasNullRole_ShouldReturnFalse() {
            // A null role never matches the ADMIN name in the exists query
            when(userRepository.existsByIdAndRole_Name(4L, "ADMIN")).thenReturn(false);

            assertThat(campaignAuthorizationService.isAdmin(4L)).isFalse();
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return false when the user does not exist")
        void isAdmin_WhenUserNotFound_ShouldReturnFalse() {
            when(userRepository.existsByIdAndRole_Name(999L, "ADMIN")).thenReturn(false);

            assertThat(campaignAuthorizationService.isAdmin(999L)).isFalse();
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return false without querying the database when the user id is null")
        void isAdmin_WhenUserIdIsNull_ShouldReturnFalseWithoutQuerying() {
            assertThat(campaignAuthorizationService.isAdmin(null)).isFalse();

            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("canCloseCampaign Tests")
    class CanCloseCampaignTests {

        @Test
        @DisplayName("Should return true when user is the campaign owner")
        void canCloseCampaign_WhenUserIsOwner_ShouldReturnTrue() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(1L)).thenReturn(Optional.of(testOwner));

            // When
            boolean result = campaignAuthorizationService.canCloseCampaign(1L, 1L);

            // Then
            assertThat(result).isTrue();

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(1L);
            verify(userRepository, times(1)).findByIdForRequest(1L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return true when user is admin")
        void canCloseCampaign_WhenUserIsAdmin_ShouldReturnTrue() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(2L)).thenReturn(Optional.of(testAdmin));

            // When
            boolean result = campaignAuthorizationService.canCloseCampaign(1L, 2L);

            // Then
            assertThat(result).isTrue();

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(1L);
            verify(userRepository, times(1)).findByIdForRequest(2L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return false when user is neither owner nor admin")
        void canCloseCampaign_WhenUserIsUnauthorized_ShouldReturnFalse() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(3L)).thenReturn(Optional.of(testRegularUser));

            // When
            boolean result = campaignAuthorizationService.canCloseCampaign(1L, 3L);

            // Then
            assertThat(result).isFalse();

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(1L);
            verify(userRepository, times(1)).findByIdForRequest(3L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return false when campaign not found")
        void canCloseCampaign_WhenCampaignNotFound_ShouldReturnFalse() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(999L)).thenReturn(Optional.empty());

            // When
            boolean result = campaignAuthorizationService.canCloseCampaign(999L, 1L);

            // Then
            assertThat(result).isFalse();

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(999L);
        }
    }

    @Nested
    @DisplayName("getCampaignIfAuthorized Tests")
    class GetCampaignIfAuthorizedTests {

        @Test
        @DisplayName("Should return campaign when user is the campaign owner")
        void getCampaignIfAuthorized_WhenUserIsOwner_ShouldReturnCampaign() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(1L)).thenReturn(Optional.of(testOwner));

            // When
            Campaign result = campaignAuthorizationService.getCampaignIfAuthorized(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test Campaign");

            verify(campaignRepository, times(2)).findByIdWithActiveOwner(1L); // Called once in validateOwnershipOrAdmin, once in getCampaignIfAuthorized
            verify(userRepository, times(1)).findByIdForRequest(1L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return campaign when user is admin")
        void getCampaignIfAuthorized_WhenUserIsAdmin_ShouldReturnCampaign() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(2L)).thenReturn(Optional.of(testAdmin));

            // When
            Campaign result = campaignAuthorizationService.getCampaignIfAuthorized(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test Campaign");

            verify(campaignRepository, times(2)).findByIdWithActiveOwner(1L); // Called once in validateOwnershipOrAdmin, once in getCampaignIfAuthorized
            verify(userRepository, times(1)).findByIdForRequest(2L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw MessageException when user is unauthorized")
        void getCampaignIfAuthorized_WhenUserIsUnauthorized_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(1L)).thenReturn(Optional.of(testCampaign));
            when(userRepository.findByIdForRequest(3L)).thenReturn(Optional.of(testRegularUser));

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.getCampaignIfAuthorized(1L, 3L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You are not authorized to perform this action");

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(1L);
            verify(userRepository, times(1)).findByIdForRequest(3L);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw MessageException when campaign not found during validation")
        void getCampaignIfAuthorized_WhenCampaignNotFoundDuringValidation_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findByIdWithActiveOwner(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignAuthorizationService.getCampaignIfAuthorized(999L, 1L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign not found with id: 999");

            verify(campaignRepository, times(1)).findByIdWithActiveOwner(999L);
            verify(userRepository, never()).findByIdForRequest(any());
        }
    }
}
