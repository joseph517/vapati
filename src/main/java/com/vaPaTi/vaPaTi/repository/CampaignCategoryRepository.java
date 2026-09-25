package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.CampaignCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CampaignCategoryRepository extends JpaRepository<CampaignCategory, Long> {
    @Query("SELECT cc FROM CampaignCategory cc JOIN FETCH cc.category WHERE cc.campaign.id = :campaignId")
    List<CampaignCategory> findByCampaignId(@Param("campaignId") Long campaignId);

    // Categories of a whole listing in one SELECT. cc.campaign comes from the persistence context: the campaigns were
    // loaded earlier in the same transaction
    @Query("SELECT cc FROM CampaignCategory cc JOIN FETCH cc.category WHERE cc.campaign.id IN :campaignIds")
    List<CampaignCategory> findByCampaignIdIn(@Param("campaignIds") Collection<Long> campaignIds);

    // Bulk delete: a derived delete would load every row (and its EAGER category) and delete them one by one
    @Modifying
    @Query("DELETE FROM CampaignCategory cc WHERE cc.campaign.id = :campaignId")
    void deleteByCampaignId(@Param("campaignId") Long campaignId);

    @Modifying
    @Query("DELETE FROM CampaignCategory cc WHERE cc.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
}
