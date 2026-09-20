package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    Boolean existsByName(String name);

    @Query("SELECT CASE WHEN COUNT(uc) > 0 THEN TRUE ELSE FALSE END " +
            "FROM UserCategory uc " +
            "WHERE uc.category.id = :categoryId")
    boolean isCategoryInUse(@Param("categoryId") Long categoryId);

    @Query("SELECT cc.campaign.id FROM CampaignCategory cc " +
            "WHERE cc.category.id = :categoryId " +
            "AND cc.campaign.id NOT IN (" +
            "  SELECT cc2.campaign.id FROM CampaignCategory cc2 WHERE cc2.category.id <> :categoryId" +
            ")")
    List<Long> findCampaignIdsThatWouldBeOrphaned(@Param("categoryId") Long categoryId);

}
