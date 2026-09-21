package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.CampaignStatus;

import java.time.LocalDateTime;

public record CampaignStatusHistoryResponseDTO(
        CampaignStatus previousStatus,
        CampaignStatus newStatus,
        Long changedByUserId,
        LocalDateTime changedAt
) {}
