package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.CampaignStatusHistory;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.repository.CampaignStatusHistoryRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignStatusHistoryService {

    private final CampaignStatusHistoryRepository campaignStatusHistoryRepository;
    private final UserRepository userRepository;

    public void recordTransition(Campaign campaign, CampaignStatus previousStatus, CampaignStatus newStatus, Long changedByUserId) {
        User changedBy = changedByUserId != null ? userRepository.findById(changedByUserId).orElse(null) : null;

        CampaignStatusHistory history = CampaignStatusHistory.builder()
                .campaign(campaign)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .build();

        campaignStatusHistoryRepository.save(history);
    }
}
