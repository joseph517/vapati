package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignServiceValidation {

    private final CampaignRepository campaignRepository;
    private final CategoryRepository categoryRepository;

    public Campaign findCampaignByIdOrThrow(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new MessageException("Campaign not found"));
    }

    public void updateCampaignFields(@NotNull Campaign campaign, @NotNull UpdateCampaignRequestDTO dto) {
        Optional.ofNullable(dto.getName()).ifPresent(campaign::setName);
        Optional.ofNullable(dto.getDescription()).ifPresent(campaign::setDescription);
    }

    public void updateGoalFields(Goal goal, UpdateCampaignRequestDTO dto) {
        if (goal == null) return;

        Optional.ofNullable(dto.getAmountGoal()).ifPresent(goal::setAmountGoal);
    }

    public CampaignStatus parseStatus(String status) {
        try {
            return CampaignStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MessageException("Invalid campaign status: " + status);
        }
    }

    public void validateCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new MessageException("At least one category must be provided");
        }
        if (categoryIds.size() > 5) {
            throw new MessageException("A campaign can have at most 5 categories");
        }
        for (Long categoryId : categoryIds) {
            if (!categoryRepository.existsById(categoryId)) {
                throw new MessageException("Category not found with id: " + categoryId);
            }
        }
    }
}
