package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignServiceValidation {

    private final CampaignRepository campaignRepository;

    public Campaign findCampaignByIdOrThrow(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
    }

    public void updateCampaignFields(@NotNull Campaign campaign, @NotNull UpdateCampaignRequestDTO dto) {
        Optional.ofNullable(dto.getName()).ifPresent(campaign::setName);
        Optional.ofNullable(dto.getDescription()).ifPresent(campaign::setDescription);
    }

    public void updateGoalFields(Goal goal, UpdateCampaignRequestDTO dto) {
        if (goal == null) return;

        Optional.ofNullable(dto.getAmountGoal()).ifPresent(goal::setAmountGoal);
        Optional.ofNullable(dto.getAmountRaised()).ifPresent(goal::setAmountRaised);
    }

}
