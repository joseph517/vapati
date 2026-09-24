package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CategoryDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignCategory;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class CampaignMapper {

    public static Campaign toEntity(@NotNull CreateCampaignRequestDTO dto, User user) {
        Goal goal = Goal.builder()
                .amountGoal(dto.getAmountGoal())
                .amountRaised(BigDecimal.ZERO)
                .status(CampaignStatus.ACTIVE)
                .build();

        return Campaign.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .user(user)
                .goal(goal)
                .build();
    }

    public static CampaignResponseDTO toResponseDTO(Campaign campaign, List<CampaignCategory> campaignCategories) {
        Goal goal = campaign.getGoal();

        List<CategoryDTO> categories = campaignCategories.stream()
                .map(campaignCategory -> {
                    CategoryDTO categoryDTO = new CategoryDTO();
                    categoryDTO.setId(campaignCategory.getCategory().getId());
                    categoryDTO.setName(campaignCategory.getCategory().getName());
                    categoryDTO.setDescription(campaignCategory.getCategory().getDescription());
                    return categoryDTO;
                })
                .toList();

        return new CampaignResponseDTO(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                goal != null ? goal.getAmountGoal() : BigDecimal.ZERO,
                goal != null ? goal.getAmountRaised() : BigDecimal.ZERO,
                campaign.getUser() != null ? campaign.getUser().getId() : null,
                categories,
                goal != null ? goal.getStatus() : null,
                campaign.getUser() != null && campaign.getUser().getUserInfo() != null
                        ? campaign.getUser().getUserInfo().getUserName() : null
        );
    }
}
