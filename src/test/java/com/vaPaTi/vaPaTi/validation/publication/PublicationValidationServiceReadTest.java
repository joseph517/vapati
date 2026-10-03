package com.vaPaTi.vaPaTi.validation.publication;

import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicationValidationService - Read Tests")
class PublicationValidationServiceReadTest {

    private static final Long CAMPAIGN_ID = 5L;
    private static final Long CALLER_ID = 2L;

    @Mock
    private CampaignValidationService campaignValidationService;

    @InjectMocks
    private PublicationValidationService publicationValidationService;

    private final Campaign campaign = Campaign.builder().id(CAMPAIGN_ID).build();

    @Test
    @DisplayName("Should delegate to findVisibleCampaignByIdOrThrow with the caller id")
    void validateAndGetReadableCampaign_WithCaller_ShouldDelegate() {
        // Given
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, CALLER_ID)).thenReturn(campaign);

        // When
        Campaign result = publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, CALLER_ID);

        // Then
        assertThat(result).isSameAs(campaign);
        verify(campaignValidationService).findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, CALLER_ID);
    }

    @Test
    @DisplayName("Should delegate with a null caller id when the caller is anonymous")
    void validateAndGetReadableCampaign_WhenAnonymous_ShouldDelegateWithNull() {
        // Given
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, null)).thenReturn(campaign);

        // When
        Campaign result = publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, null);

        // Then
        assertThat(result).isSameAs(campaign);
        verify(campaignValidationService).findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, null);
    }

    @Test
    @DisplayName("Should propagate the 404 when the campaign is not visible")
    void validateAndGetReadableCampaign_WhenNotVisible_ShouldThrowNotFound() {
        // Given
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, null))
                .thenThrow(new ResourceNotFoundException("Campaign not found with id: " + CAMPAIGN_ID));

        // When & Then
        assertThatThrownBy(() -> publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Campaign not found with id: " + CAMPAIGN_ID);
    }
}
