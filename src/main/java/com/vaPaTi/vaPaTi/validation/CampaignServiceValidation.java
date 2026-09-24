package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
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
    private final CampaignAuthorizationService campaignAuthorizationService;

    public Campaign findCampaignByIdOrThrow(Long campaignId) {
        return campaignRepository.findByIdWithActiveOwner(campaignId)
                .orElseThrow(() -> campaignNotFound(campaignId));
    }

    // callerId may be null (anonymous). A CLOSED campaign of someone else gets the same 404 as a missing one
    public Campaign findVisibleCampaignByIdOrThrow(Long campaignId, Long callerId) {
        Optional<Campaign> campaign = campaignAuthorizationService.isAdmin(callerId)
                ? campaignRepository.findByIdWithActiveOwner(campaignId)
                : campaignRepository.findByIdVisibleTo(campaignId, callerId);
        return campaign.orElseThrow(() -> campaignNotFound(campaignId));
    }

    private ResourceNotFoundException campaignNotFound(Long campaignId) {
        return new ResourceNotFoundException("Campaign not found with id: " + campaignId);
    }

    public void updateCampaignFields(@NotNull Campaign campaign, @NotNull UpdateCampaignRequestDTO dto) {
        Optional.ofNullable(dto.getName()).ifPresent(campaign::setName);
        Optional.ofNullable(dto.getDescription()).ifPresent(campaign::setDescription);
    }

    public void updateGoalFields(Goal goal, UpdateCampaignRequestDTO dto) {
        if (goal == null) return;

        Optional.ofNullable(dto.getAmountGoal()).ifPresent(goal::setAmountGoal);
    }

    // Status a non-CLOSED goal should have for its amounts: COMPLETED once the amount raised reaches the goal
    public CampaignStatus statusForAmounts(@NotNull Goal goal) {
        return goal.getAmountRaised().compareTo(goal.getAmountGoal()) >= 0
                ? CampaignStatus.COMPLETED
                : CampaignStatus.ACTIVE;
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
                throw new ResourceNotFoundException("Category not found with id: " + categoryId);
            }
        }
    }
}
