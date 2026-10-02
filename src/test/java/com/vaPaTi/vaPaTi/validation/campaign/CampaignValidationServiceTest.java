package com.vaPaTi.vaPaTi.validation.campaign;

import com.vaPaTi.vaPaTi.dtos.campaign.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.category.Category;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.campaign.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.category.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignValidationService - Unit Tests")
class CampaignValidationServiceTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CampaignAuthorizationService campaignAuthorizationService;

    @InjectMocks
    private CampaignValidationService campaignValidationService;

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
                .amountGoal(new BigDecimal("1000.0"))
                .amountRaised(new BigDecimal("500.0"))
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
                new BigDecimal("2000.0"),
                List.of(1L)
        );
    }

    @DisplayName("findCampaignByIdOrThrow - Should return campaign when found")
    @Test
    void findCampaignByIdOrThrow_WhenCampaignExists_ShouldReturnCampaign() {
        // Given
        Long campaignId = 1L;
        when(campaignRepository.findByIdWithActiveOwner(campaignId)).thenReturn(Optional.of(testCampaign));

        // When
        Campaign result = campaignValidationService.findCampaignByIdOrThrow(campaignId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(campaignId);
        assertThat(result.getName()).isEqualTo("Test Campaign");
        assertThat(result.getDescription()).isEqualTo("Test Description");

        verify(campaignRepository, times(1)).findByIdWithActiveOwner(campaignId);
        verifyNoMoreInteractions(campaignRepository);
    }

    @DisplayName("findCampaignByIdOrThrow - Should throw RuntimeException when campaign not found")
    @Test
    void findCampaignByIdOrThrow_WhenCampaignNotFound_ShouldThrowRuntimeException() {
        // Given
        Long campaignId = 999L;
        when(campaignRepository.findByIdWithActiveOwner(campaignId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> campaignValidationService.findCampaignByIdOrThrow(campaignId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Campaign not found with id: " + campaignId);

        verify(campaignRepository, times(1)).findByIdWithActiveOwner(campaignId);
        verifyNoMoreInteractions(campaignRepository);
    }

    @DisplayName("findCampaignByIdOrThrow - Should throw ResourceNotFoundException when the owner is deleted")
    @Test
    void findCampaignByIdOrThrow_WhenOwnerIsDeleted_ShouldThrowResourceNotFoundException() {
        // Given: the owner filter excludes the campaign, even though findById would still return it
        Long campaignId = 1L;
        when(campaignRepository.findByIdWithActiveOwner(campaignId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> campaignValidationService.findCampaignByIdOrThrow(campaignId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Campaign not found with id: " + campaignId);

        verify(campaignRepository, never()).findById(any());
    }

    @DisplayName("findCampaignByIdOrThrow - Should handle null campaignId gracefully")
    @Test
    void findCampaignByIdOrThrow_WhenCampaignIdIsNull_ShouldCallRepositoryWithNull() {
        // Given
        when(campaignRepository.findByIdWithActiveOwner(null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> campaignValidationService.findCampaignByIdOrThrow(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Campaign not found with id: null");

        verify(campaignRepository, times(1)).findByIdWithActiveOwner(null);
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
        campaignValidationService.updateCampaignFields(campaign, testDTO);

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
        campaignValidationService.updateCampaignFields(campaign, dtoWithOnlyName);

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
        campaignValidationService.updateCampaignFields(campaign, dtoWithOnlyDescription);

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
                null, null, new BigDecimal("1000.0"), List.of(1L)
        );

        // When
        campaignValidationService.updateCampaignFields(campaign, dtoWithNullFields);

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
        campaignValidationService.updateCampaignFields(campaign, dtoWithEmptyStrings);

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
                .amountGoal(new BigDecimal("1000.0"))
                .amountRaised(new BigDecimal("500.0"))
                .build();

        UpdateCampaignRequestDTO dtoWithOnlyAmountGoal = new UpdateCampaignRequestDTO(
                null, null, new BigDecimal("3000.0"), List.of(1L)
        );

        // When
        campaignValidationService.updateGoalFields(goal, dtoWithOnlyAmountGoal);

        // Then
        assertThat(goal.getAmountGoal()).isEqualByComparingTo(new BigDecimal("3000.0"));
        assertThat(goal.getAmountRaised()).isEqualByComparingTo(new BigDecimal("500.0"));
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateGoalFields - Should not update any amount when both are null")
    @Test
    void updateGoalFields_WhenBothAmountsAreNull_ShouldNotUpdateAnyAmount() {
        // Given
        Goal goal = Goal.builder()
                .amountGoal(new BigDecimal("1000.0"))
                .amountRaised(new BigDecimal("500.0"))
                .build();

        UpdateCampaignRequestDTO dtoWithNullAmounts = new UpdateCampaignRequestDTO(
                "Name", "Description", null, List.of(1L)
        );

        // When
        campaignValidationService.updateGoalFields(goal, dtoWithNullAmounts);

        // Then
        assertThat(goal.getAmountGoal()).isEqualByComparingTo(new BigDecimal("1000.0"));
        // amountRaised should not be updated (no longer in DTO)
        assertThat(goal.getAmountRaised()).isEqualByComparingTo(new BigDecimal("500.0"));
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateGoalFields - Should return early when goal is null")
    @Test
    void updateGoalFields_WhenGoalIsNull_ShouldReturnEarlyWithoutException() {
        // Given
        Goal nullGoal = null;

        // When & Then (should not throw exception)
        campaignValidationService.updateGoalFields(nullGoal, testDTO);

        // Then
        verifyNoInteractions(campaignRepository);
        // Test passes if no exception is thrown
    }

    @DisplayName("updateGoalFields - Should handle zero amounts as valid values")
    @Test
    void updateGoalFields_WhenZeroAmountsProvided_ShouldUpdateWithZeroValues() {
        // Given
        Goal goal = Goal.builder()
                .amountGoal(new BigDecimal("1000.0"))
                .amountRaised(new BigDecimal("500.0"))
                .build();

        UpdateCampaignRequestDTO dtoWithZeroAmounts = new UpdateCampaignRequestDTO(
                null, null, new BigDecimal("0.0"), List.of(1L)
        );

        // When
        campaignValidationService.updateGoalFields(goal, dtoWithZeroAmounts);

        // Then
        assertThat(goal.getAmountGoal()).isEqualByComparingTo(new BigDecimal("0.0"));
        // amountRaised should not be updated (no longer in DTO)
        assertThat(goal.getAmountRaised()).isEqualByComparingTo(new BigDecimal("500.0"));
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("updateGoalFields - Should handle negative amounts as valid values")
    @Test
    void updateGoalFields_WhenNegativeAmountsProvided_ShouldUpdateWithNegativeValues() {
        // Given
        Goal goal = Goal.builder()
                .amountGoal(new BigDecimal("1000.0"))
                .amountRaised(new BigDecimal("500.0"))
                .build();

        UpdateCampaignRequestDTO dtoWithNegativeAmounts = new UpdateCampaignRequestDTO(
                null, null, new BigDecimal("-100.0"), List.of(1L)
        );

        // When
        campaignValidationService.updateGoalFields(goal, dtoWithNegativeAmounts);

        // Then
        assertThat(goal.getAmountGoal()).isEqualByComparingTo(new BigDecimal("-100.0"));
        // amountRaised should not be updated (no longer in DTO)
        assertThat(goal.getAmountRaised()).isEqualByComparingTo(new BigDecimal("500.0"));
        verifyNoInteractions(campaignRepository);
    }

    @DisplayName("parseStatus - Should return the matching enum value for a valid status")
    @Test
    void parseStatus_WithValidValue_ShouldReturnEnum() {
        // When & Then
        assertThat(campaignValidationService.parseStatus("ACTIVE")).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(campaignValidationService.parseStatus("completed")).isEqualTo(CampaignStatus.COMPLETED);
        assertThat(campaignValidationService.parseStatus("Closed")).isEqualTo(CampaignStatus.CLOSED);
    }

    @DisplayName("parseStatus - Should throw MessageException when value does not match any enum constant")
    @Test
    void parseStatus_WithInvalidValue_ShouldThrow() {
        // When & Then
        assertThatThrownBy(() -> campaignValidationService.parseStatus("FOO"))
                .isInstanceOf(MessageException.class)
                .hasMessage("Invalid campaign status: FOO");
    }

    private static Category category(Long id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    @DisplayName("validateAndGetCategories - Should return the categories of a single findAllById when all ids exist")
    @Test
    void validateAndGetCategories_WithValidIds_ShouldReturnCategories() {
        // Given
        List<Long> categoryIds = List.of(1L, 2L, 3L);
        List<Category> categories = List.of(category(1L), category(2L), category(3L));
        when(categoryRepository.findAllById(categoryIds)).thenReturn(categories);

        // When
        List<Category> result = campaignValidationService.validateAndGetCategories(categoryIds);

        // Then
        assertThat(result).isSameAs(categories);
        verify(categoryRepository, times(1)).findAllById(categoryIds);
        verify(categoryRepository, never()).existsById(any());
    }

    @DisplayName("validateAndGetCategories - Should return findAllById's result as is, with repeated ids in the request")
    @Test
    void validateAndGetCategories_WithRepeatedIds_ShouldReturnFindAllByIdResult() {
        // Given: findAllById returns each category once and in its own order
        List<Long> categoryIds = List.of(2L, 2L, 1L);
        List<Category> categories = List.of(category(1L), category(2L));
        when(categoryRepository.findAllById(categoryIds)).thenReturn(categories);

        // When
        List<Category> result = campaignValidationService.validateAndGetCategories(categoryIds);

        // Then
        assertThat(result).isSameAs(categories);
    }

    @DisplayName("validateAndGetCategories - Should throw when the list is null")
    @Test
    void validateAndGetCategories_WithNullList_ShouldThrow() {
        // When & Then
        assertThatThrownBy(() -> campaignValidationService.validateAndGetCategories(null))
                .isInstanceOf(MessageException.class)
                .hasMessage("At least one category must be provided");

        verifyNoInteractions(categoryRepository);
    }

    @DisplayName("validateAndGetCategories - Should throw when the list is empty")
    @Test
    void validateAndGetCategories_WithEmptyList_ShouldThrow() {
        // When & Then
        assertThatThrownBy(() -> campaignValidationService.validateAndGetCategories(List.of()))
                .isInstanceOf(MessageException.class)
                .hasMessage("At least one category must be provided");

        verifyNoInteractions(categoryRepository);
    }

    @DisplayName("validateAndGetCategories - Should throw when more than 5 ids are provided")
    @Test
    void validateAndGetCategories_WithMoreThanFiveIds_ShouldThrow() {
        // Given
        List<Long> categoryIds = List.of(1L, 2L, 3L, 4L, 5L, 6L);

        // When & Then
        assertThatThrownBy(() -> campaignValidationService.validateAndGetCategories(categoryIds))
                .isInstanceOf(MessageException.class)
                .hasMessage("A campaign can have at most 5 categories");

        verifyNoInteractions(categoryRepository);
    }

    @DisplayName("validateAndGetCategories - Should throw ResourceNotFoundException when a category id does not exist")
    @Test
    void validateAndGetCategories_WithNonExistentId_ShouldThrow() {
        // Given
        List<Long> categoryIds = List.of(1L, 999L);
        when(categoryRepository.findAllById(categoryIds)).thenReturn(List.of(category(1L)));

        // When & Then
        assertThatThrownBy(() -> campaignValidationService.validateAndGetCategories(categoryIds))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 999");
        verify(categoryRepository, never()).existsById(any());
    }

    @DisplayName("validateAndGetCategories - Should report the first missing id in request order")
    @Test
    void validateAndGetCategories_WithSeveralNonExistentIds_ShouldReportFirstInRequestOrder() {
        // Given
        List<Long> categoryIds = List.of(998L, 1L, 999L);
        when(categoryRepository.findAllById(categoryIds)).thenReturn(List.of(category(1L)));

        // When & Then
        assertThatThrownBy(() -> campaignValidationService.validateAndGetCategories(categoryIds))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 998");
    }

    @Nested
    @DisplayName("findVisibleCampaignById Tests")
    class FindVisibleCampaignByIdTests {

        private static final Long CAMPAIGN_ID = 1L;
        private static final Long CALLER_ID = 3L;

        @Test
        @DisplayName("Should return the campaign when it is visible to the caller")
        void findVisibleCampaignById_WhenVisible_ShouldReturnCampaign() {
            // Given
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(false);
            when(campaignRepository.findByIdVisibleTo(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.of(testCampaign));

            // When
            Optional<Campaign> result = campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, CALLER_ID);

            // Then
            assertThat(result).containsSame(testCampaign);
        }

        @Test
        @DisplayName("Should return empty instead of throwing when the campaign is not visible")
        void findVisibleCampaignById_WhenNotVisible_ShouldReturnEmpty() {
            // Given: a CLOSED campaign of someone else is filtered out by the query
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(false);
            when(campaignRepository.findByIdVisibleTo(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.empty());

            // When
            Optional<Campaign> result = campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, CALLER_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when an admin looks up a missing campaign")
        void findVisibleCampaignById_WhenAdminAndMissing_ShouldReturnEmpty() {
            // Given
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(true);
            when(campaignRepository.findByIdWithActiveOwner(CAMPAIGN_ID)).thenReturn(Optional.empty());

            // When
            Optional<Campaign> result = campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, CALLER_ID);

            // Then
            assertThat(result).isEmpty();
            verify(campaignRepository, never()).findByIdVisibleTo(any(), any());
        }
    }

    @Nested
    @DisplayName("findVisibleCampaignByIdOrThrow Tests")
    class FindVisibleCampaignByIdOrThrowTests {

        private static final Long CAMPAIGN_ID = 1L;
        private static final Long CALLER_ID = 3L;

        @Test
        @DisplayName("Should use the unrestricted query when the caller is an admin")
        void findVisibleCampaignByIdOrThrow_WhenAdmin_ShouldUseFindByIdWithActiveOwner() {
            // Given
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(true);
            when(campaignRepository.findByIdWithActiveOwner(CAMPAIGN_ID)).thenReturn(Optional.of(testCampaign));

            // When
            Campaign result = campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, CALLER_ID);

            // Then
            assertThat(result).isSameAs(testCampaign);
            verify(campaignRepository, never()).findByIdVisibleTo(any(), any());
        }

        @Test
        @DisplayName("Should use the visibility query with the caller id when the caller is not an admin")
        void findVisibleCampaignByIdOrThrow_WhenNotAdmin_ShouldUseFindByIdVisibleTo() {
            // Given
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(false);
            when(campaignRepository.findByIdVisibleTo(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.of(testCampaign));

            // When
            Campaign result = campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, CALLER_ID);

            // Then
            assertThat(result).isSameAs(testCampaign);
            verify(campaignRepository, never()).findByIdWithActiveOwner(any());
        }

        @Test
        @DisplayName("Should use the visibility query with a null caller id when the caller is anonymous")
        void findVisibleCampaignByIdOrThrow_WhenAnonymous_ShouldUseFindByIdVisibleToWithNull() {
            // Given
            when(campaignAuthorizationService.isAdmin(null)).thenReturn(false);
            when(campaignRepository.findByIdVisibleTo(CAMPAIGN_ID, null)).thenReturn(Optional.of(testCampaign));

            // When
            Campaign result = campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, null);

            // Then
            assertThat(result).isSameAs(testCampaign);
            verify(campaignRepository, never()).findByIdWithActiveOwner(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException with the not-found message when the campaign is not visible")
        void findVisibleCampaignByIdOrThrow_WhenNotVisible_ShouldThrowResourceNotFoundException() {
            // Given: a CLOSED campaign of someone else is filtered out by the query
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(false);
            when(campaignRepository.findByIdVisibleTo(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.empty());

            // When & Then: same message as findCampaignByIdOrThrow for a missing id
            assertThatThrownBy(() -> campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, CALLER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Campaign not found with id: " + CAMPAIGN_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when an admin looks up a missing campaign")
        void findVisibleCampaignByIdOrThrow_WhenAdminAndMissing_ShouldThrowResourceNotFoundException() {
            // Given
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(true);
            when(campaignRepository.findByIdWithActiveOwner(CAMPAIGN_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, CALLER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Campaign not found with id: " + CAMPAIGN_ID);
        }
    }

    @Nested
    @DisplayName("statusForAmounts() tests")
    class StatusForAmountsTests {

        private Goal goalWith(String amountGoal, String amountRaised) {
            return Goal.builder()
                    .amountGoal(new BigDecimal(amountGoal))
                    .amountRaised(new BigDecimal(amountRaised))
                    .build();
        }

        @Test
        @DisplayName("Should be ACTIVE when the amount raised is below the goal")
        void statusForAmounts_WhenRaisedBelowGoal_ShouldBeActive() {
            assertThat(campaignValidationService.statusForAmounts(goalWith("100.00", "99.99")))
                    .isEqualTo(CampaignStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should be COMPLETED when the amount raised equals the goal, regardless of the scale")
        void statusForAmounts_WhenRaisedEqualsGoal_ShouldBeCompleted() {
            assertThat(campaignValidationService.statusForAmounts(goalWith("100.0", "100.00")))
                    .isEqualTo(CampaignStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should be COMPLETED when the amount raised is above the goal")
        void statusForAmounts_WhenRaisedAboveGoal_ShouldBeCompleted() {
            assertThat(campaignValidationService.statusForAmounts(goalWith("100.00", "150.00")))
                    .isEqualTo(CampaignStatus.COMPLETED);
        }
    }
}
