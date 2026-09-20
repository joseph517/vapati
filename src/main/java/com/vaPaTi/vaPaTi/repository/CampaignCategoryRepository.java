package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.CampaignCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignCategoryRepository extends JpaRepository<CampaignCategory, Long> {
    List<CampaignCategory> findByCampaignId(Long campaignId);
    List<CampaignCategory> findByCategoryId(Long categoryId);
    void deleteByCampaignId(Long campaignId);
}
