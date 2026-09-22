package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.CampaignCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignCategoryRepository extends JpaRepository<CampaignCategory, Long> {
    List<CampaignCategory> findByCampaignId(Long campaignId);
    void deleteByCampaignId(Long campaignId);

    @Modifying
    @Query("DELETE FROM CampaignCategory cc WHERE cc.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
}
