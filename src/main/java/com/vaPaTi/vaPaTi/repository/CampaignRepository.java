package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByUserId(Long userId);

    @Query("SELECT c FROM Campaign c JOIN c.user u WHERE u.deletedAt IS NULL")
    List<Campaign> findAllWithActiveOwner();

    @Query("SELECT c FROM Campaign c JOIN c.user u WHERE u.deletedAt IS NULL AND c.goal.status = :status")
    List<Campaign> findByGoalStatusWithActiveOwner(@Param("status") CampaignStatus status);

    @Query("SELECT c FROM Campaign c JOIN c.user u WHERE u.deletedAt IS NULL AND c.id = :id")
    Optional<Campaign> findByIdWithActiveOwner(@Param("id") Long id);
}

