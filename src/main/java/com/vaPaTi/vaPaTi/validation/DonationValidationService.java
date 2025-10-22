package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.CreateDonationDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonationValidationService {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

    public void validateInput(@NotNull CreateDonationDTO dto) {
        if (dto.getCampaignId() == null) {
            throw new MessageException("Campaign ID is required");
        }
        if (dto.getAmount() == null) {
            throw new MessageException("Amount is required");
        }
        if (dto.getAmount() <= 0) {
            throw new MessageException("Amount must be greater than zero");
        }
    }

    public Campaign validateAndGetCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new MessageException("Campaign not found with id: " + campaignId));

        if (campaign.getDeletedAt() != null) {
            throw new MessageException("Cannot donate to a deleted campaign");
        }

        return campaign;
    }

    public void validateGoalIsActive(Goal goal) {
        if (goal == null) {
            throw new MessageException("Campaign does not have a goal");
        }
        if (Boolean.FALSE.equals(goal.getActive())) {
            throw new MessageException("Campaign goal is not active");
        }
    }

    public User validateAndGetDonor(Long donorUserId) {
        return userRepository.findById(donorUserId)
                .orElseThrow(() -> new MessageException("Donor user not found with id: " + donorUserId));
    }

    public void validateNotSelfDonation(Long donorUserId, Long campaignOwnerId) {
        if (donorUserId.equals(campaignOwnerId)) {
            throw new MessageException("Cannot donate to your own campaign");
        }
    }
}
