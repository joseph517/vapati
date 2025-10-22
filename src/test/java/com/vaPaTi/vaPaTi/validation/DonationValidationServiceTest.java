package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.CreateDonationDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Goal;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationValidationService - Unit Tests")
class DonationValidationServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DonationValidationService donationValidationService;

    private Campaign testCampaign;
    private Goal testGoal;
    private User testUser;
    private CreateDonationDTO testDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .build();

        testGoal = Goal.builder()
                .id(1L)
                .amountGoal(1000.0)
                .amountRaised(500.0)
                .active(true)
                .build();

        testCampaign = Campaign.builder()
                .id(1L)
                .name("Test Campaign")
                .description("Test Description")
                .user(testUser)
                .goal(testGoal)
                .deletedAt(null)
                .build();

        testDTO = new CreateDonationDTO();
        testDTO.setCampaignId(1L);
        testDTO.setAmount(100.0);
    }

    @Nested
    @DisplayName("validateInput Tests")
    class ValidateInputTests {

        @Test
        @DisplayName("Should validate successfully with valid input")
        void validateInput_WhenValidInput_ShouldNotThrowException() {
            // Given
            CreateDonationDTO validDTO = new CreateDonationDTO();
            validDTO.setCampaignId(1L);
            validDTO.setAmount(100.0);

            // When & Then (should not throw exception)
            donationValidationService.validateInput(validDTO);
        }

        @Test
        @DisplayName("Should throw MessageException when campaign ID is null")
        void validateInput_WhenCampaignIdIsNull_ShouldThrowMessageException() {
            // Given
            CreateDonationDTO dtoWithNullCampaignId = new CreateDonationDTO();
            dtoWithNullCampaignId.setCampaignId(null);
            dtoWithNullCampaignId.setAmount(100.0);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateInput(dtoWithNullCampaignId))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign ID is required");
        }

        @Test
        @DisplayName("Should throw MessageException when amount is null")
        void validateInput_WhenAmountIsNull_ShouldThrowMessageException() {
            // Given
            CreateDonationDTO dtoWithNullAmount = new CreateDonationDTO();
            dtoWithNullAmount.setCampaignId(1L);
            dtoWithNullAmount.setAmount(null);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateInput(dtoWithNullAmount))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Amount is required");
        }

        @Test
        @DisplayName("Should throw MessageException when amount is zero")
        void validateInput_WhenAmountIsZero_ShouldThrowMessageException() {
            // Given
            CreateDonationDTO dtoWithZeroAmount = new CreateDonationDTO();
            dtoWithZeroAmount.setCampaignId(1L);
            dtoWithZeroAmount.setAmount(0.0);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateInput(dtoWithZeroAmount))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Amount must be greater than zero");
        }

        @Test
        @DisplayName("Should throw MessageException when amount is negative")
        void validateInput_WhenAmountIsNegative_ShouldThrowMessageException() {
            // Given
            CreateDonationDTO dtoWithNegativeAmount = new CreateDonationDTO();
            dtoWithNegativeAmount.setCampaignId(1L);
            dtoWithNegativeAmount.setAmount(-50.0);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateInput(dtoWithNegativeAmount))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Amount must be greater than zero");
        }
    }

    @Nested
    @DisplayName("validateAndGetCampaign Tests")
    class ValidateAndGetCampaignTests {

        @Test
        @DisplayName("Should return campaign when campaign exists and is not deleted")
        void validateAndGetCampaign_WhenCampaignExistsAndNotDeleted_ShouldReturnCampaign() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));

            // When
            Campaign result = donationValidationService.validateAndGetCampaign(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test Campaign");
            assertThat(result.getDeletedAt()).isNull();

            verify(campaignRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should throw MessageException when campaign not found")
        void validateAndGetCampaign_WhenCampaignNotFound_ShouldThrowMessageException() {
            // Given
            when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndGetCampaign(999L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign not found with id: 999");

            verify(campaignRepository, times(1)).findById(999L);
        }

        @Test
        @DisplayName("Should throw MessageException when campaign is deleted")
        void validateAndGetCampaign_WhenCampaignIsDeleted_ShouldThrowMessageException() {
            // Given
            testCampaign.setDeletedAt(LocalDateTime.now());
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndGetCampaign(1L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot donate to a deleted campaign");

            verify(campaignRepository, times(1)).findById(1L);
        }
    }

    @Nested
    @DisplayName("validateGoalIsActive Tests")
    class ValidateGoalIsActiveTests {

        @Test
        @DisplayName("Should validate successfully when goal is active")
        void validateGoalIsActive_WhenGoalIsActive_ShouldNotThrowException() {
            // Given
            Goal activeGoal = Goal.builder()
                    .id(1L)
                    .active(true)
                    .build();

            // When & Then (should not throw exception)
            donationValidationService.validateGoalIsActive(activeGoal);
        }

        @Test
        @DisplayName("Should throw MessageException when goal is null")
        void validateGoalIsActive_WhenGoalIsNull_ShouldThrowMessageException() {
            // Given
            Goal nullGoal = null;

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateGoalIsActive(nullGoal))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign does not have a goal");
        }

        @Test
        @DisplayName("Should throw MessageException when goal is not active")
        void validateGoalIsActive_WhenGoalIsNotActive_ShouldThrowMessageException() {
            // Given
            Goal inactiveGoal = Goal.builder()
                    .id(1L)
                    .active(false)
                    .build();

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateGoalIsActive(inactiveGoal))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign goal is not active");
        }
    }

    @Nested
    @DisplayName("validateAndGetDonor Tests")
    class ValidateAndGetDonorTests {

        @Test
        @DisplayName("Should return user when donor exists")
        void validateAndGetDonor_WhenDonorExists_ShouldReturnUser() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            User result = donationValidationService.validateAndGetDonor(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            verify(userRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should throw MessageException when donor not found")
        void validateAndGetDonor_WhenDonorNotFound_ShouldThrowMessageException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndGetDonor(999L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Donor user not found with id: 999");

            verify(userRepository, times(1)).findById(999L);
        }
    }

    @Nested
    @DisplayName("validateNotSelfDonation Tests")
    class ValidateNotSelfDonationTests {

        @Test
        @DisplayName("Should validate successfully when donor and owner are different users")
        void validateNotSelfDonation_WhenDifferentUsers_ShouldNotThrowException() {
            // Given
            Long donorUserId = 1L;
            Long campaignOwnerId = 2L;

            // When & Then (should not throw exception)
            donationValidationService.validateNotSelfDonation(donorUserId, campaignOwnerId);
        }

        @Test
        @DisplayName("Should throw MessageException when donor and owner are the same user")
        void validateNotSelfDonation_WhenSameUser_ShouldThrowMessageException() {
            // Given
            Long sameUserId = 1L;

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateNotSelfDonation(sameUserId, sameUserId))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot donate to your own campaign");
        }
    }
}
