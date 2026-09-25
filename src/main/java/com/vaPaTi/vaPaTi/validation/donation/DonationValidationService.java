package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonationValidationService {

    private final CampaignValidationService campaignValidationService;
    private final UserRepository userRepository;

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
}
