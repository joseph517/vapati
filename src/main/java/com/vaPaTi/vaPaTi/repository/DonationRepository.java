package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Donation;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.validation.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
