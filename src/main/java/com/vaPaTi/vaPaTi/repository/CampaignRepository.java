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

    // Listings load the owner, its inverse @OneToOne (userInfo for userName, verificationRequest) and the goal in the
    // same SELECT. goal is a LEFT JOIN: a campaign whose goal is soft-deleted keeps showing up with goal = null
    String LISTING_FETCH = "JOIN FETCH c.user u LEFT JOIN FETCH u.userInfo LEFT JOIN FETCH u.verificationRequest " +
            "LEFT JOIN FETCH c.goal ";

    // Used by closeAllByOwner, which doesn't need the fetches
    List<Campaign> findByUserId(Long userId);

    @Query("SELECT c FROM Campaign c " + LISTING_FETCH + "WHERE u.id = :userId")
    List<Campaign> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT c FROM Campaign c " + LISTING_FETCH + "WHERE u.deletedAt IS NULL")
    List<Campaign> findAllWithActiveOwner();

    @Query("SELECT c FROM Campaign c " + LISTING_FETCH + "WHERE u.deletedAt IS NULL AND c.goal.status = :status")
    List<Campaign> findByGoalStatusWithActiveOwner(@Param("status") CampaignStatus status);

    @Query("SELECT c FROM Campaign c " + LISTING_FETCH + "WHERE u.deletedAt IS NULL AND EXISTS (" +
            "SELECT 1 FROM CampaignCategory cc WHERE cc.campaign.id = c.id AND cc.category.id = :categoryId)")
    List<Campaign> findByCategoryIdWithActiveOwner(@Param("categoryId") Long categoryId);

    // *VisibleTo queries are for non-admins: CLOSED campaigns only for their owner. A null callerId (anonymous) never matches u.id
    @Query("SELECT c FROM Campaign c " + LISTING_FETCH + "WHERE u.deletedAt IS NULL " +
            "AND (c.goal.status <> com.vaPaTi.vaPaTi.entity.CampaignStatus.CLOSED OR u.id = :callerId)")
    List<Campaign> findAllVisibleTo(@Param("callerId") Long callerId);

    @Query("SELECT c FROM Campaign c " + LISTING_FETCH + "WHERE u.deletedAt IS NULL AND c.goal.status = :status " +
            "AND (c.goal.status <> com.vaPaTi.vaPaTi.entity.CampaignStatus.CLOSED OR u.id = :callerId)")
    List<Campaign> findByGoalStatusVisibleTo(@Param("status") CampaignStatus status, @Param("callerId") Long callerId);

    @Query("SELECT c FROM Campaign c " + LISTING_FETCH + "WHERE u.deletedAt IS NULL AND EXISTS (" +
            "SELECT 1 FROM CampaignCategory cc WHERE cc.campaign.id = c.id AND cc.category.id = :categoryId) " +
            "AND (c.goal.status <> com.vaPaTi.vaPaTi.entity.CampaignStatus.CLOSED OR u.id = :callerId)")
    List<Campaign> findByCategoryIdVisibleTo(@Param("categoryId") Long categoryId, @Param("callerId") Long callerId);

    // Single-campaign loads use the same fetches as the listings
    @Query("SELECT c FROM Campaign c " + LISTING_FETCH + "WHERE u.deletedAt IS NULL AND c.id = :id")
    Optional<Campaign> findByIdWithActiveOwner(@Param("id") Long id);

    @Query("SELECT c FROM Campaign c " + LISTING_FETCH + "WHERE u.deletedAt IS NULL AND c.id = :id " +
            "AND (c.goal.status <> com.vaPaTi.vaPaTi.entity.CampaignStatus.CLOSED OR u.id = :callerId)")
    Optional<Campaign> findByIdVisibleTo(@Param("id") Long id, @Param("callerId") Long callerId);

    @Query(value = "SELECT id, name FROM campaign WHERE id IN (:ids)", nativeQuery = true)
    List<CampaignNameProjection> findNamesByIdsIncludingDeleted(@Param("ids") List<Long> ids);

    interface CampaignNameProjection {
        Long getId();

        String getName();
    }
}

