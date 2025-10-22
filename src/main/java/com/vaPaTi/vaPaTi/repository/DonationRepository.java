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

    List<Donation> findByCampaignOrderByCreatedAtDesc(Campaign campaign);

    List<Donation> findByStatus(DonationStatus status);

    @Query("SELECT d FROM Donation d WHERE d.campaign.id = :campaignId ORDER BY d.createdAt DESC")
    List<Donation> findByCampaignIdOrderByCreatedAtDesc(@Param("campaignId") Long campaignId);

    @Query("SELECT d FROM Donation d WHERE d.donor.id = :donorId ORDER BY d.createdAt DESC")
    List<Donation> findByDonorIdOrderByCreatedAtDesc(@Param("donorId") Long donorId);

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.campaign.id = :campaignId AND d.status = 'COMPLETED'")
    Double sumCompletedDonationsByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(DISTINCT d.donor.id) FROM Donation d WHERE d.campaign.id = :campaignId AND d.status = 'COMPLETED'")
    Long countUniqueDonorsByCampaignId(@Param("campaignId") Long campaignId);
}
