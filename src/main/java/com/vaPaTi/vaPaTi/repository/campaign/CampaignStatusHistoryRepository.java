package com.vaPaTi.vaPaTi.repository.campaign;

import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignStatusHistoryRepository extends JpaRepository<CampaignStatusHistory, Long> {
    List<CampaignStatusHistory> findByCampaignIdOrderByChangedAtAsc(Long campaignId);
}
