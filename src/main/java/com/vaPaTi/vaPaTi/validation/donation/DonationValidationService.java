package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.donation.DonationStatus;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonationValidationService {

    private static final int MAX_MESSAGE_LENGTH = 280;

    private final CampaignValidationService campaignValidationService;
    private final UserRepository userRepository;
    private final TextValidationService textValidationService;
    private final DonationRepository donationRepository;
    private final CampaignAuthorizationService campaignAuthorizationService;

    public void validateInput(@NotNull CreateDonationDTO dto) {
        if (dto.getCampaignId() == null) {
            throw new MessageException("Campaign ID is required");
        }
        if (dto.getAmount() == null) {
            throw new MessageException("Amount is required");
        }
        if (dto.getAmount().signum() <= 0) {
            throw new MessageException("Amount must be greater than zero");
        }
    }

    // null means no message. The length check repeats the DTO's @Size, as validateInput does with @NotNull
    public String validateAndNormalizeMessage(String message) {
        if (message == null) {
            return null;
        }

        return textValidationService.normalizeRequiredText(message, "Message", MAX_MESSAGE_LENGTH);
    }

    // callerId may be null (anonymous). A CLOSED campaign of someone else is reported as not found
    public Campaign validateAndGetCampaign(Long campaignId, Long callerId) {
        Campaign campaign = campaignValidationService.findVisibleCampaignByIdOrThrow(campaignId, callerId);

        if (campaign.getDeletedAt() != null) {
            throw new MessageException("Cannot donate to a deleted campaign");
        }

        return campaign;
    }

    public void validateGoalIsActive(Goal goal) {
        if (goal == null) {
            throw new MessageException("Campaign does not have a goal");
        }
        if (goal.getStatus() == CampaignStatus.CLOSED) {
            throw new MessageException("Campaign goal is not active");
        }
    }

    public User validateAndGetDonor(Long donorUserId) {
        return userRepository.findById(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor user not found with id: " + donorUserId));
    }

    public void validateNotSelfDonation(Long donorUserId, Long campaignOwnerId) {
        if (donorUserId.equals(campaignOwnerId)) {
            throw new MessageException("Cannot donate to your own campaign");
        }
    }

    public String validateAndNormalizeThanksMessage(String thanksMessage) {
        return textValidationService.normalizeRequiredText(thanksMessage, "Thanks message", MAX_MESSAGE_LENGTH);
    }

    public Donation validateAndGetDonation(Long donationId) {
        return donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));
    }

    // A soft-deleted campaign leaves the relation null (@NotFound), so its donations can't be replied to
    public Campaign validateAndGetDonationCampaign(Donation donation) {
        Campaign campaign = donation.getCampaign();
        if (campaign == null) {
            throw new MessageException("Cannot reply to a donation of a deleted campaign");
        }
        return campaign;
    }

    // Only the owner replies: an ADMIN can delete a reply but not write one
    public void validateIsCampaignOwner(Campaign campaign, Long callerId) {
        if (!isCampaignOwner(campaign, callerId)) {
            throw new ForbiddenActionException("Only the campaign owner can reply to this donation");
        }
    }

    public void validateIsCampaignOwnerOrAdmin(Campaign campaign, Long callerId) {
        if (!isCampaignOwner(campaign, callerId) && !campaignAuthorizationService.isAdmin(callerId)) {
            throw new ForbiddenActionException("Only the campaign owner or an admin can delete this reply");
        }
    }

    public void validateDonationCompleted(Donation donation) {
        if (donation.getStatus() != DonationStatus.COMPLETED) {
            throw new MessageException("Only completed donations can be replied to");
        }
    }

    // The reply is not editable: it has to be deleted before replying again
    public void validateHasNoThanks(Donation donation) {
        if (donation.getThanksMessage() != null) {
            throw new ConflictException("Donation already has a thanks message");
        }
    }

    public void validateHasThanks(Donation donation) {
        if (donation.getThanksMessage() == null) {
            throw new ResourceNotFoundException("Donation has no thanks message");
        }
    }

    private boolean isCampaignOwner(Campaign campaign, Long callerId) {
        return callerId != null && campaign.getUser() != null && callerId.equals(campaign.getUser().getId());
    }
}
