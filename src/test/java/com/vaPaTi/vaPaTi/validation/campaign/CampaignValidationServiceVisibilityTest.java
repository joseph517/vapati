package com.vaPaTi.vaPaTi.validation.campaign;

import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.repository.campaign.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.category.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignValidationService - Visibility Tests")
class CampaignValidationServiceVisibilityTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CampaignAuthorizationService campaignAuthorizationService;

    @InjectMocks
    private CampaignValidationService campaignValidationService;

    private Campaign campaignWithStatus(CampaignStatus status) {
        return Campaign.builder()
                .id(1L)
                .goal(Goal.builder().id(1L).status(status).build())
                .build();
    }

    @Nested
    @DisplayName("isPublicCampaign")
    class IsPublicCampaign {

        @ParameterizedTest
        @EnumSource(value = CampaignStatus.class, names = {"ACTIVE", "COMPLETED"})
        @DisplayName("Should be public when the goal is ACTIVE or COMPLETED")
        void isPublicCampaign_ActiveOrCompleted_ShouldReturnTrue(CampaignStatus status) {
            // When
            boolean result = campaignValidationService.isPublicCampaign(campaignWithStatus(status));

            // Then
            assertThat(result).isTrue();
            verifyNoInteractions(campaignRepository, campaignAuthorizationService);
        }

        @Test
        @DisplayName("Should not be public when the goal is CLOSED")
        void isPublicCampaign_Closed_ShouldReturnFalse() {
            // When
            boolean result = campaignValidationService.isPublicCampaign(campaignWithStatus(CampaignStatus.CLOSED));

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should not be public when the campaign is null")
        void isPublicCampaign_NullCampaign_ShouldReturnFalse() {
            // When
            boolean result = campaignValidationService.isPublicCampaign(null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should not be public when the campaign has no goal")
        void isPublicCampaign_NullGoal_ShouldReturnFalse() {
            // Given
            Campaign campaign = Campaign.builder().id(1L).goal(null).build();

            // When
            boolean result = campaignValidationService.isPublicCampaign(campaign);

            // Then
            assertThat(result).isFalse();
        }
    }
}
