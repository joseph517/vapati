package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import org.jetbrains.annotations.NotNull;

public class CampaignMapper {

    public static Campaign toEntity(@NotNull CreateCampaignRequestDTO dto, User user) {
        Goal goal = Goal.builder()
                .amountGoal(dto.getAmountGoal())
                .amountRaised(dto.getAmountRaised() != null ? dto.getAmountRaised() : 0)
                .build();

        return Campaign.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .user(user)
                .goal(goal)
                .build();
    }

    public static CampaignResponseDTO toResponseDTO(Campaign campaign) {
        Goal goal = campaign.getGoal();

        return new CampaignResponseDTO(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                goal != null ? goal.getAmountGoal() : 0,
                goal != null ? goal.getAmountRaised() : 0,
                campaign.getUser() != null ? campaign.getUser().getId() : null
        );
    }
}
