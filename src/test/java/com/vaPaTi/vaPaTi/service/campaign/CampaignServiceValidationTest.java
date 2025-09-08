package com.vaPaTi.vaPaTi.service.campaign;

import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.validation.CampaignServiceValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("CampaignServiceValidation - Unit Tests")
class CampaignServiceValidationTest {

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private CampaignServiceValidation campaignServiceValidation;

    private Campaign testCampaign;
    private Goal testGoal;
    private User testUser;
    private UpdateCampaignRequestDTO testDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .build();

        testGoal = Goal.builder()
                .id(1L)
                .amountGoal(1000.0)
                .amountRaised(500.0)
                .build();

        testCampaign = Campaign.builder()
                .id(1L)
                .user(testUser)
                .name("Test Campaign")
                .description("Test Description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .goal(testGoal)
                .build();

        testDTO = new UpdateCampaignRequestDTO(
                "Updated Campaign Name",
                "Updated Description",
                2000.0,
                1500.0
        );
    }

    @DisplayName("findCampaignByIdOrThrow - Should return campaign when found")
    @Test
    void findCampaignByIdOrThrow_WhenCampaignExists_ShouldReturnCampaign() {
        // Given
        Long campaignId = 1L;
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(testCampaign));

        // When
        Campaign result = campaignServiceValidation.findCampaignByIdOrThrow(campaignId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(campaignId);
        assertThat(result.getName()).isEqualTo("Test Campaign");
        assertThat(result.getDescription()).isEqualTo("Test Description");

        verify(campaignRepository, times(1)).findById(campaignId);
        verifyNoMoreInteractions(campaignRepository);
    }

    @DisplayName("findCampaignByIdOrThrow - Should throw RuntimeException when campaign not found")
    @Test
    void findCampaignByIdOrThrow_WhenCampaignNotFound_ShouldThrowRuntimeException() {
        // Given
        Long campaignId = 999L;
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> campaignServiceValidation.findCampaignByIdOrThrow(campaignId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Campaign not found");

        verify(campaignRepository, times(1)).findById(campaignId);
        verifyNoMoreInteractions(campaignRepository);
    }

    @DisplayName("findCampaignByIdOrThrow - Should handle null campaignId gracefully")
    @Test
    void findCampaignByIdOrThrow_WhenCampaignIdIsNull_ShouldCallRepositoryWithNull() {
        // Given
        when(campaignRepository.findById(null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> campaignServiceValidation.findCampaignByIdOrThrow(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Campaign not found");

        verify(campaignRepository, times(1)).findById(null);
        verifyNoMoreInteractions(campaignRepository);
    }

    @DisplayName("updateCampaignFields - Should update both name and description when both are provided")
    @Test
    void updateCampaignFields_WhenBothFieldsProvided_ShouldUpdateBothFields() {
        // Given
        Campaign campaign = Campaign.builder()
                .name("Original Name")
                .description("Original Description")
                .build();

        // When
        campaignServiceValidation.updateCampaignFields(campaign, testDTO);

        // Then
        assertThat(campaign.getName()).isEqualTo("Updated Campaign Name");
        assertThat(campaign.getDescription()).isEqualTo("Updated Description");
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateCampaignFields - Should update only name when only name is provided")
    @Test
    void updateCampaignFields_WhenOnlyNameProvided_ShouldUpdateOnlyName() {
        // Given
        Campaign campaign = Campaign.builder()
                .name("Original Name")
                .description("Original Description")
                .build();

        UpdateCampaignRequestDTO dtoWithOnlyName = new UpdateCampaignRequestDTO(
                "New Name", null, null, null
        );

        // When
        campaignServiceValidation.updateCampaignFields(campaign, dtoWithOnlyName);

        // Then
        assertThat(campaign.getName()).isEqualTo("New Name");
        assertThat(campaign.getDescription()).isEqualTo("Original Description");
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateCampaignFields - Should update only description when only description is provided")
    @Test
    void updateCampaignFields_WhenOnlyDescriptionProvided_ShouldUpdateOnlyDescription() {
        // Given
        Campaign campaign = Campaign.builder()
                .name("Original Name")
                .description("Original Description")
                .build();

        UpdateCampaignRequestDTO dtoWithOnlyDescription = new UpdateCampaignRequestDTO(
                null, "New Description", null, null
        );

        // When
        campaignServiceValidation.updateCampaignFields(campaign, dtoWithOnlyDescription);

        // Then
        assertThat(campaign.getName()).isEqualTo("Original Name");
        assertThat(campaign.getDescription()).isEqualTo("New Description");
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateCampaignFields - Should not update any field when both are null")
    @Test
    void updateCampaignFields_WhenBothFieldsAreNull_ShouldNotUpdateAnyField() {
        // Given
        Campaign campaign = Campaign.builder()
                .name("Original Name")
                .description("Original Description")
                .build();

        UpdateCampaignRequestDTO dtoWithNullFields = new UpdateCampaignRequestDTO(
                null, null, 1000.0, 500.0
        );

        // When
        campaignServiceValidation.updateCampaignFields(campaign, dtoWithNullFields);

        // Then
        assertThat(campaign.getName()).isEqualTo("Original Name");
        assertThat(campaign.getDescription()).isEqualTo("Original Description");
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateCampaignFields - Should handle empty strings as valid values")
    @Test
    void updateCampaignFields_WhenEmptyStringsProvided_ShouldUpdateWithEmptyStrings() {
        // Given
        Campaign campaign = Campaign.builder()
                .name("Original Name")
                .description("Original Description")
                .build();

        UpdateCampaignRequestDTO dtoWithEmptyStrings = new UpdateCampaignRequestDTO(
                "", "", null, null
        );

        // When
        campaignServiceValidation.updateCampaignFields(campaign, dtoWithEmptyStrings);

        // Then
        assertThat(campaign.getName()).isEqualTo("");
        assertThat(campaign.getDescription()).isEqualTo("");
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateGoalFields - Should update both amount fields when both are provided and goal exists")
    @Test
    void updateGoalFields_WhenBothAmountsProvidedAndGoalExists_ShouldUpdateBothAmounts() {
        // Given
        Goal goal = Goal.builder()
                .amountGoal(1000.0)
                .amountRaised(500.0)
                .build();

        // When
        campaignServiceValidation.updateGoalFields(goal, testDTO);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(2000.0);
        assertThat(goal.getAmountRaised()).isEqualTo(1500.0);
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateGoalFields - Should update only amount goal when only amount goal is provided")
    @Test
    void updateGoalFields_WhenOnlyAmountGoalProvided_ShouldUpdateOnlyAmountGoal() {
        // Given
        Goal goal = Goal.builder()
                .amountGoal(1000.0)
                .amountRaised(500.0)
                .build();

        UpdateCampaignRequestDTO dtoWithOnlyAmountGoal = new UpdateCampaignRequestDTO(
                null, null, 3000.0, null
        );

        // When
        campaignServiceValidation.updateGoalFields(goal, dtoWithOnlyAmountGoal);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(3000.0);
        assertThat(goal.getAmountRaised()).isEqualTo(500.0);
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateGoalFields - Should update only amount raised when only amount raised is provided")
    @Test
    void updateGoalFields_WhenOnlyAmountRaisedProvided_ShouldUpdateOnlyAmountRaised() {
        // Given
        Goal goal = Goal.builder()
                .amountGoal(1000.0)
                .amountRaised(500.0)
                .build();

        UpdateCampaignRequestDTO dtoWithOnlyAmountRaised = new UpdateCampaignRequestDTO(
                null, null, null, 800.0
        );

        // When
        campaignServiceValidation.updateGoalFields(goal, dtoWithOnlyAmountRaised);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(1000.0);
        assertThat(goal.getAmountRaised()).isEqualTo(800.0);
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateGoalFields - Should not update any amount when both are null")
    @Test
    void updateGoalFields_WhenBothAmountsAreNull_ShouldNotUpdateAnyAmount() {
        // Given
        Goal goal = Goal.builder()
                .amountGoal(1000.0)
                .amountRaised(500.0)
                .build();

        UpdateCampaignRequestDTO dtoWithNullAmounts = new UpdateCampaignRequestDTO(
                "Name", "Description", null, null
        );

        // When
        campaignServiceValidation.updateGoalFields(goal, dtoWithNullAmounts);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(1000.0);
        assertThat(goal.getAmountRaised()).isEqualTo(500.0);
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateGoalFields - Should return early when goal is null")
    @Test
    void updateGoalFields_WhenGoalIsNull_ShouldReturnEarlyWithoutException() {
        // Given
        Goal nullGoal = null;

        // When & Then (should not throw exception)
        campaignServiceValidation.updateGoalFields(nullGoal, testDTO);

        // Then
        verifyNoInteractions(campaignRepository);
        // Test passes if no exception is thrown
    }

    @DisplayName("updateGoalFields - Should handle zero amounts as valid values")
    @Test
    void updateGoalFields_WhenZeroAmountsProvided_ShouldUpdateWithZeroValues() {
        // Given
        Goal goal = Goal.builder()
                .amountGoal(1000.0)
                .amountRaised(500.0)
                .build();

        UpdateCampaignRequestDTO dtoWithZeroAmounts = new UpdateCampaignRequestDTO(
                null, null, 0.0, 0.0
        );

        // When
        campaignServiceValidation.updateGoalFields(goal, dtoWithZeroAmounts);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(0.0);
        assertThat(goal.getAmountRaised()).isEqualTo(0.0);
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateGoalFields - Should handle negative amounts as valid values")
    @Test
    void updateGoalFields_WhenNegativeAmountsProvided_ShouldUpdateWithNegativeValues() {
        // Given
        Goal goal = Goal.builder()
                .amountGoal(1000.0)
                .amountRaised(500.0)
                .build();

        UpdateCampaignRequestDTO dtoWithNegativeAmounts = new UpdateCampaignRequestDTO(
                null, null, -100.0, -50.0
        );

        // When
        campaignServiceValidation.updateGoalFields(goal, dtoWithNegativeAmounts);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(-100.0);
        assertThat(goal.getAmountRaised()).isEqualTo(-50.0);
        verifyNoInteractions(campaignRepository);
    }

}
