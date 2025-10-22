package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignStatisticsDTO;
import com.vaPaTi.vaPaTi.dtos.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Donation;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.mapper.DonationMapper;
import com.vaPaTi.vaPaTi.repository.DonationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.DonationStatus;
import com.vaPaTi.vaPaTi.validation.DonationValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final DonationValidationService donationValidationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final DonationMapper donationMapper;

    @Transactional
    public DonationResponseDTO createDonation(@NotNull CreateDonationDTO dto) {
        // Get authenticated user
        Long donorUserId = authenticatedUserService.getAuthenticatedUserId();

        // Validate input
        donationValidationService.validateInput(dto);

        // Validate campaign exists and is not deleted
        Campaign campaign = donationValidationService.validateAndGetCampaign(dto.getCampaignId());

        // Validate goal is active
        Goal goal = campaign.getGoal();
        donationValidationService.validateGoalIsActive(goal);

        // Validate donor user exists
        User donor = donationValidationService.validateAndGetDonor(donorUserId);

        // Validate not self-donation
        donationValidationService.validateNotSelfDonation(donorUserId, campaign.getUser().getId());

        // Create donation with PENDING status
        Donation donation = Donation.builder()
                .donor(donor)
                .campaign(campaign)
                .amount(dto.getAmount())
                .status(DonationStatus.PENDING)
                .build();

        // Auto-approve: System simulates validation
        // In production, this would integrate with payment gateway
        donation.setStatus(DonationStatus.COMPLETED);
        donation.setTransactionId(generateTransactionId());

        // Update goal amount raised
        goal.setAmountRaised(goal.getAmountRaised() + dto.getAmount());

        // Save donation (goal will be updated via cascade)
        Donation savedDonation = donationRepository.save(donation);

        return donationMapper.toDTO(savedDonation);
    }

    public List<DonationResponseDTO> getDonationsByAuthenticatedUser() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        List<Donation> donations = donationRepository.findByDonorIdOrderByCreatedAtDesc(userId);
        return donationMapper.toDTOList(donations);
    }

    public List<DonationResponseDTO> getDonationsByCampaign(Long campaignId) {
        Campaign campaign = donationValidationService.validateAndGetCampaign(campaignId);
        List<Donation> donations = donationRepository.findByCampaignOrderByCreatedAtDesc(campaign);
        return donationMapper.toDTOList(donations);
    }

    public CampaignStatisticsDTO getCampaignStatistics(Long campaignId) {
        Campaign campaign = donationValidationService.validateAndGetCampaign(campaignId);

        Double totalRaised = donationRepository.sumCompletedDonationsByCampaignId(campaignId);
        Long uniqueDonors = donationRepository.countUniqueDonorsByCampaignId(campaignId);

        // Handle null values from queries
        totalRaised = totalRaised != null ? totalRaised : 0.0;
        uniqueDonors = uniqueDonors != null ? uniqueDonors : 0L;

        Goal goal = campaign.getGoal();
        Double goalAmount = goal != null ? goal.getAmountGoal() : 0.0;
        Double percentage = goalAmount > 0 ? (totalRaised / goalAmount) * 100 : 0.0;

        return CampaignStatisticsDTO.builder()
                .campaignId(campaignId)
                .campaignName(campaign.getName())
                .amountGoal(goalAmount)
                .amountRaised(totalRaised)
                .percentageReached(percentage)
                .isGoalReached(totalRaised >= goalAmount)
                .isActive(goal != null && goal.getActive())
                .totalDonors(uniqueDonors)
                .build();
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
