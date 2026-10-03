package com.vaPaTi.vaPaTi.validation.publication;

import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicationValidationService - Create Tests")
class PublicationValidationServiceCreateTest {

    private static final Long CAMPAIGN_ID = 5L;
    private static final Long OWNER_ID = 1L;
    private static final Long THIRD_PARTY_ID = 2L;
    private static final Long ADMIN_ID = 3L;

    @Mock
    private CampaignValidationService campaignValidationService;

    @InjectMocks
    private PublicationValidationService publicationValidationService;

    private Campaign campaign;
    private Goal goal;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(OWNER_ID).build();
        goal = Goal.builder().status(CampaignStatus.ACTIVE).build();
        campaign = Campaign.builder().id(CAMPAIGN_ID).user(owner).goal(goal).build();
    }

    @Test
    @DisplayName("Should return the campaign to its owner when it is ACTIVE")
    void validateAndGetPublishableCampaign_WhenOwnerAndActive_ShouldReturnCampaign() {
        // Given
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, OWNER_ID)).thenReturn(campaign);

        // When
        Campaign result = publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, OWNER_ID);

        // Then
        assertThat(result).isSameAs(campaign);
    }

    @Test
    @DisplayName("Should return the campaign to its owner when it is COMPLETED")
    void validateAndGetPublishableCampaign_WhenOwnerAndCompleted_ShouldReturnCampaign() {
        // Given
        goal.setStatus(CampaignStatus.COMPLETED);
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, OWNER_ID)).thenReturn(campaign);

        // When
        Campaign result = publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, OWNER_ID);

        // Then
        assertThat(result).isSameAs(campaign);
    }

    @Test
    @DisplayName("Should throw MessageException (400) to the owner when the campaign is CLOSED")
    void validateAndGetPublishableCampaign_WhenOwnerAndClosed_ShouldThrowMessageException() {
        // Given
        goal.setStatus(CampaignStatus.CLOSED);
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, OWNER_ID)).thenReturn(campaign);

        // When & Then
        assertThatThrownBy(() -> publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, OWNER_ID))
                .isExactlyInstanceOf(MessageException.class)
                .hasMessage("Cannot publish in a closed campaign");
    }

    @Test
    @DisplayName("Should throw ForbiddenActionException (403) to a third party")
    void validateAndGetPublishableCampaign_WhenThirdParty_ShouldThrowForbidden() {
        // Given
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, THIRD_PARTY_ID)).thenReturn(campaign);

        // When & Then
        assertThatThrownBy(() -> publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, THIRD_PARTY_ID))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only the campaign owner can publish in it");
    }

    @Test
    @DisplayName("Should throw ForbiddenActionException (403) to an admin who is not the owner")
    void validateAndGetPublishableCampaign_WhenAdminNotOwner_ShouldThrowForbidden() {
        // Given: an admin sees the campaign, but only the owner can publish
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, ADMIN_ID)).thenReturn(campaign);

        // When & Then
        assertThatThrownBy(() -> publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, ADMIN_ID))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only the campaign owner can publish in it");
    }

    @Test
    @DisplayName("Should check the owner before the status, so a non-owner never learns the campaign is CLOSED")
    void validateAndGetPublishableCampaign_WhenNotOwnerAndClosed_ShouldThrowForbiddenFirst() {
        // Given: an admin sees a CLOSED campaign of someone else
        goal.setStatus(CampaignStatus.CLOSED);
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, ADMIN_ID)).thenReturn(campaign);

        // When & Then
        assertThatThrownBy(() -> publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, ADMIN_ID))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only the campaign owner can publish in it");
    }

    @Test
    @DisplayName("Should propagate the 404 when the campaign is not visible to the caller")
    void validateAndGetPublishableCampaign_WhenNotVisible_ShouldThrowNotFound() {
        // Given: a CLOSED campaign of someone else, a missing one or one of a deleted owner
        when(campaignValidationService.findVisibleCampaignByIdOrThrow(CAMPAIGN_ID, THIRD_PARTY_ID))
                .thenThrow(new ResourceNotFoundException("Campaign not found with id: " + CAMPAIGN_ID));

        // When & Then
        assertThatThrownBy(() -> publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, THIRD_PARTY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Campaign not found with id: " + CAMPAIGN_ID);
    }
}
