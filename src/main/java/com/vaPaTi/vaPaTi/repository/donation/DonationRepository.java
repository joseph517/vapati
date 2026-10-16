package com.vaPaTi.vaPaTi.repository.donation;

import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.donation.DonationStatus;
import com.vaPaTi.vaPaTi.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByDonor(User donor);

    List<Donation> findByDonorOrderByCreatedAtDesc(User donor);

    List<Donation> findByCampaign(Campaign campaign);

    // Loads each donor with its inverse @OneToOne in the same SELECT. All joins are LEFT: a soft-deleted donor stays
    // null (@NotFound) instead of removing the row. d.campaign comes from the persistence context
    @Query("SELECT d FROM Donation d LEFT JOIN FETCH d.donor du LEFT JOIN FETCH du.userInfo " +
            "LEFT JOIN FETCH du.verificationRequest WHERE d.campaign = :campaign ORDER BY d.createdAt DESC")
    List<Donation> findByCampaignOrderByCreatedAtDesc(@Param("campaign") Campaign campaign);

    List<Donation> findByStatus(DonationStatus status);

    @Query("SELECT d FROM Donation d WHERE d.campaignId = :campaignId ORDER BY d.createdAt DESC")
    List<Donation> findByCampaignIdOrderByCreatedAtDesc(@Param("campaignId") Long campaignId);

    // Loads the donor and the campaign (with its goal and owner) in the same SELECT. All joins are LEFT: a
    // soft-deleted donor or campaign stays null (@NotFound) instead of removing the row
    @Query("SELECT d FROM Donation d LEFT JOIN FETCH d.donor du LEFT JOIN FETCH du.userInfo " +
            "LEFT JOIN FETCH du.verificationRequest LEFT JOIN FETCH d.campaign c LEFT JOIN FETCH c.goal " +
            "LEFT JOIN FETCH c.user cu LEFT JOIN FETCH cu.userInfo LEFT JOIN FETCH cu.verificationRequest " +
            "WHERE d.donorUserId = :donorId ORDER BY d.createdAt DESC")
    List<Donation> findByDonorIdOrderByCreatedAtDesc(@Param("donorId") Long donorId);

    @Query("SELECT COUNT(d) FROM Donation d WHERE d.campaignId = :campaignId AND d.status = 'COMPLETED'")
    Long countCompletedDonationsByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(DISTINCT d.donorUserId) FROM Donation d WHERE d.campaignId = :campaignId AND d.status = 'COMPLETED'")
    Long countUniqueDonorsByCampaignId(@Param("campaignId") Long campaignId);

    // Native: the @SQLRestriction of campaign and [user] does not apply, so deleted campaigns and donors are counted.
    // SQL Server can't use an alias inside an ORDER BY expression nor parametrize ASC/DESC: one CASE per field and
    // direction, the ones that don't apply are NULL on every row. donor_user_id ASC is the fixed tie-breaker
    @Query(nativeQuery = true, value = """
            SELECT d.donor_user_id AS donorUserId, SUM(d.amount) AS totalAmount,
                   COUNT_BIG(*) AS donationsCount, MAX(d.created_at) AS lastDonationAt
            FROM donation d JOIN campaign c ON c.id = d.campaign_id
            WHERE c.user_id = :creatorId AND d.status = 'COMPLETED'
            GROUP BY d.donor_user_id
            ORDER BY
              CASE WHEN :sortBy = 'totalAmount'    AND :descending = 1 THEN SUM(d.amount) END DESC,
              CASE WHEN :sortBy = 'totalAmount'    AND :descending = 0 THEN SUM(d.amount) END ASC,
              CASE WHEN :sortBy = 'lastDonationAt' AND :descending = 1 THEN MAX(d.created_at) END DESC,
              CASE WHEN :sortBy = 'lastDonationAt' AND :descending = 0 THEN MAX(d.created_at) END ASC,
              d.donor_user_id ASC
            OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
            """)
    List<SupporterProjection> findSupportersByCreatorId(@Param("creatorId") Long creatorId, @Param("sortBy") String sortBy,
                                                        @Param("descending") boolean descending,
                                                        @Param("offset") long offset, @Param("size") int size);

    @Query(nativeQuery = true, value = """
            SELECT COUNT_BIG(DISTINCT d.donor_user_id)
            FROM donation d JOIN campaign c ON c.id = d.campaign_id
            WHERE c.user_id = :creatorId AND d.status = 'COMPLETED'
            """)
    long countSupportersByCreatorId(@Param("creatorId") Long creatorId);

    interface SupporterProjection {
        Long getDonorUserId();

        BigDecimal getTotalAmount();

        Long getDonationsCount();

        LocalDateTime getLastDonationAt();
    }
}
