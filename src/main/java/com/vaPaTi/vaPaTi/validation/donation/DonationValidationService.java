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
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonationValidationService {

    private static final int MAX_MESSAGE_LENGTH = 280;
    private static final String RECIPIENT_ROLE = "USER";

    private final CampaignValidationService campaignValidationService;
    private final UserRepository userRepository;
    private final TextValidationService textValidationService;
    private final DonationRepository donationRepository;
    private final CampaignAuthorizationService campaignAuthorizationService;
    private final UserValidationService userValidationService;
    private final RoleValidationService roleValidationService;

    // Exactly one destination: a campaign or a user (direct donation). Checked before the amount
    public void validateInput(@NotNull CreateDonationDTO dto) {
        if (dto.getCampaignId() == null && dto.getRecipientUserId() == null) {
            throw new MessageException("Campaign ID or recipient user ID is required");
        }
        if (dto.getCampaignId() != null && dto.getRecipientUserId() != null) {
            throw new MessageException("Only one of campaignId or recipientUserId can be set");
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

    // Recipient of a direct donation. Missing, deleted, banned or suspended is "User not found", also for an
    // ADMIN donor, so a sanction is not revealed. The role is LAZY: read it inside the caller's transaction
    public User validateAndGetRecipient(Long recipientUserId) {
        User recipient = userValidationService.getUserById(recipientUserId);
        userValidationService.validateProfileNotBlocked(recipient);

        if (!RECIPIENT_ROLE.equals(recipient.getRole().getName())) {
            throw new MessageException("Cannot donate to an admin");
        }

        return recipient;
    }

    public void validateNotSelfDirectDonation(Long donorUserId, Long recipientUserId) {
        if (donorUserId.equals(recipientUserId)) {
            throw new MessageException("Cannot donate to yourself");
        }
    }

    public String validateAndNormalizeThanksMessage(String thanksMessage) {
        return textValidationService.normalizeRequiredText(thanksMessage, "Thanks message", MAX_MESSAGE_LENGTH);
    }

    public Donation validateAndGetDonation(Long donationId) {
        return donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));
    }

    // A soft-deleted campaign leaves the relation null (@NotFound) while campaignId keeps the id, so its donations
    // can't be replied to. A direct donation has no campaign at all and passes
    public void validateDonationCampaignNotDeleted(Donation donation) {
        if (donation.getCampaignId() != null && donation.getCampaign() == null) {
            throw new MessageException("Cannot reply to a donation of a deleted campaign");
        }
    }

    // Only for a campaign donation (campaignId set). A soft-deleted campaign leaves the relation null (@NotFound)
    // and is not public; otherwise the campaign domain decides, so the CLOSED rule has a single source
    public boolean isCampaignPublic(Donation donation) {
        return donation.getCampaign() != null
                && campaignValidationService.isPublicCampaign(donation.getCampaign());
    }

    // Only the recipient replies (the campaign owner in a campaign donation): an ADMIN can delete a reply but not write one
    public void validateIsRecipient(Donation donation, Long callerId) {
        if (!isRecipient(donation, callerId)) {
            throw new ForbiddenActionException(isCampaignDonation(donation)
                    ? "Only the campaign owner can reply to this donation"
                    : "Only the recipient can reply to this donation");
        }
    }

    public void validateIsRecipientOrAdmin(Donation donation, Long callerId) {
        if (!isRecipient(donation, callerId) && !roleValidationService.isAdmin(callerId)) {
            throw new ForbiddenActionException(isCampaignDonation(donation)
                    ? "Only the campaign owner or an admin can delete this reply"
                    : "Only the recipient or an admin can delete this reply");
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

    // Sorting by date would reveal who donated most recently, which the public view hides
    public void validateSupporterSort(String sortBy, boolean fullView) {
        if ("lastDonationAt".equals(sortBy) && !fullView) {
            throw new MessageException("Only the creator or an admin can sort supporters by lastDonationAt");
        }
    }

    private boolean isCampaignDonation(Donation donation) {
        return donation.getCampaignId() != null;
    }

    private boolean isRecipient(Donation donation, Long callerId) {
        return callerId != null && callerId.equals(donation.getRecipientUserId());
    }
}
