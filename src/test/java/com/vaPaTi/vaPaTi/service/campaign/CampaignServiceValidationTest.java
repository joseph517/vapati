package com.vaPaTi.vaPaTi.service.campaign;

import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import com.vaPaTi.vaPaTi.validation.CampaignServiceValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignServiceValidation - Unit Tests")
class CampaignServiceValidationTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CategoryRepository categoryRepository;

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
                List.of(1L)
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
                "New Name", null, null, List.of(1L)
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
                null, "New Description", null, List.of(1L)
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
                null, null, 1000.0, List.of(1L)
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
                "", "", null, List.of(1L)
        );

        // When
        campaignServiceValidation.updateCampaignFields(campaign, dtoWithEmptyStrings);

        // Then
        assertThat(campaign.getName()).isEqualTo("");
        assertThat(campaign.getDescription()).isEqualTo("");
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
                null, null, 3000.0, List.of(1L)
        );

        // When
        campaignServiceValidation.updateGoalFields(goal, dtoWithOnlyAmountGoal);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(3000.0);
        assertThat(goal.getAmountRaised()).isEqualTo(500.0);
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
                "Name", "Description", null, List.of(1L)
        );

        // When
        campaignServiceValidation.updateGoalFields(goal, dtoWithNullAmounts);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(1000.0);
        // amountRaised should not be updated (no longer in DTO)
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
                null, null, 0.0, List.of(1L)
        );

        // When
        campaignServiceValidation.updateGoalFields(goal, dtoWithZeroAmounts);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(0.0);
        // amountRaised should not be updated (no longer in DTO)
        assertThat(goal.getAmountRaised()).isEqualTo(500.0);
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
                null, null, -100.0, List.of(1L)
        );

        // When
        campaignServiceValidation.updateGoalFields(goal, dtoWithNegativeAmounts);

        // Then
        assertThat(goal.getAmountGoal()).isEqualTo(-100.0);
        // amountRaised should not be updated (no longer in DTO)
        assertThat(goal.getAmountRaised()).isEqualTo(500.0);
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("validateCategoryIds - Should pass when between 1 and 5 existing category ids are provided")
    @Test
    void validateCategoryIds_WithValidIds_ShouldNotThrow() {
        // Given
        List<Long> categoryIds = List.of(1L, 2L, 3L);
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(2L)).thenReturn(true);
        when(categoryRepository.existsById(3L)).thenReturn(true);

        // When & Then
        assertThatCode(() -> campaignServiceValidation.validateCategoryIds(categoryIds))
                .doesNotThrowAnyException();

        verify(categoryRepository).existsById(1L);
        verify(categoryRepository).existsById(2L);
        verify(categoryRepository).existsById(3L);
    }

    @DisplayName("validateCategoryIds - Should throw when the list is null")
    @Test
    void validateCategoryIds_WithNullList_ShouldThrow() {
        // When & Then
        assertThatThrownBy(() -> campaignServiceValidation.validateCategoryIds(null))
                .isInstanceOf(MessageException.class)
                .hasMessage("At least one category must be provided");

        verifyNoInteractions(categoryRepository);
    }

    @DisplayName("validateCategoryIds - Should throw when the list is empty")
    @Test
    void validateCategoryIds_WithEmptyList_ShouldThrow() {
        // When & Then
        assertThatThrownBy(() -> campaignServiceValidation.validateCategoryIds(List.of()))
                .isInstanceOf(MessageException.class)
                .hasMessage("At least one category must be provided");

        verifyNoInteractions(categoryRepository);
    }

    @DisplayName("validateCategoryIds - Should throw when more than 5 ids are provided")
    @Test
    void validateCategoryIds_WithMoreThanFiveIds_ShouldThrow() {
        // Given
        List<Long> categoryIds = List.of(1L, 2L, 3L, 4L, 5L, 6L);

        // When & Then
        assertThatThrownBy(() -> campaignServiceValidation.validateCategoryIds(categoryIds))
                .isInstanceOf(MessageException.class)
                .hasMessage("A campaign can have at most 5 categories");

        verifyNoInteractions(categoryRepository);
    }

    @DisplayName("validateCategoryIds - Should throw when a category id does not exist")
    @Test
    void validateCategoryIds_WithNonExistentId_ShouldThrow() {
        // Given
        List<Long> categoryIds = List.of(1L, 999L);
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> campaignServiceValidation.validateCategoryIds(categoryIds))
                .isInstanceOf(MessageException.class)
                .hasMessage("Category not found with id: 999");
    }

}
