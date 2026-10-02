package com.vaPaTi.vaPaTi.validation.publication;

import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.publication.PublicationRepository;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicationValidationService {

    private static final String PUBLICATION_NOT_FOUND = "Publication not found with ID: ";

    private final PublicationRepository publicationRepository;
    private final CampaignValidationService campaignValidationService;

    // Visibility first, so a third party gets a 404 for a CLOSED campaign of someone else (spec 19).
    // Then the owner, also for an admin, and last the status, so only the owner learns the campaign is CLOSED
    public Campaign validateAndGetPublishableCampaign(Long campaignId, Long callerId) {
        Campaign campaign = campaignValidationService.findVisibleCampaignByIdOrThrow(campaignId, callerId);

        if (!campaign.getUser().getId().equals(callerId)) {
            throw new ForbiddenActionException("Only the campaign owner can publish in it");
        }

        if (campaign.getGoal() != null && campaign.getGoal().getStatus() == CampaignStatus.CLOSED) {
            throw new MessageException("Cannot publish in a closed campaign");
        }

        return campaign;
    }

    // callerId may be null (anonymous)
    public Campaign validateAndGetReadableCampaign(Long campaignId, Long callerId) {
        return campaignValidationService.findVisibleCampaignByIdOrThrow(campaignId, callerId);
    }

    public Publication validateAndGetOwnedPublication(Long publicationId, Long userId) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException(PUBLICATION_NOT_FOUND + publicationId));

        // A publication whose author was soft-deleted is hidden (spec 18)
        if (publication.getUser() == null) {
            throw new ResourceNotFoundException(PUBLICATION_NOT_FOUND + publicationId);
        }

        if (!publication.getUser().getId().equals(userId)) {
            throw new ForbiddenActionException("You don't have permission to delete this publication");
        }

        return publication;
    }
}
