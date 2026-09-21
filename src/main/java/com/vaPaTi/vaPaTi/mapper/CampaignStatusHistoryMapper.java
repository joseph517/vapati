package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.CampaignStatusHistoryResponseDTO;
import com.vaPaTi.vaPaTi.entity.CampaignStatusHistory;

public class CampaignStatusHistoryMapper {

    public static CampaignStatusHistoryResponseDTO toResponseDTO(CampaignStatusHistory entity) {
        return new CampaignStatusHistoryResponseDTO(
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getChangedBy() != null ? entity.getChangedBy().getId() : null,
                entity.getChangedAt()
        );
    }
}
