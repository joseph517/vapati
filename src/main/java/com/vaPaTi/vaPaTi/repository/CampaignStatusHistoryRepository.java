package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.CampaignStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignStatusHistoryRepository extends JpaRepository<CampaignStatusHistory, Long> {
    List<CampaignStatusHistory> findByCampaignIdOrderByChangedAtAsc(Long campaignId);
}
