package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignStatisticsDTO;
import com.vaPaTi.vaPaTi.dtos.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Donation;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.DonationMapper;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.DonationRepository;
import com.vaPaTi.vaPaTi.repository.GoalRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.DonationStatus;
import com.vaPaTi.vaPaTi.validation.DonationValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonationService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final DonationRepository donationRepository;
    private final DonationValidationService donationValidationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final DonationMapper donationMapper;
    private final CampaignStatusHistoryService campaignStatusHistoryService;
    private final CampaignRepository campaignRepository;
    private final GoalRepository goalRepository;

    @Transactional
    public DonationResponseDTO createDonation(@NotNull CreateDonationDTO dto) {
        // Get authenticated user
        Long donorUserId = authenticatedUserService.getAuthenticatedUserId();

        // Validate input
        donationValidationService.validateInput(dto);

        // Validate campaign exists, is visible to the donor and is not deleted (before the status check)
        Campaign campaign = donationValidationService.validateAndGetCampaign(dto.getCampaignId(), donorUserId);

        // Validate goal is active
        Goal goal = campaign.getGoal();
        donationValidationService.validateGoalIsActive(goal);

        // Validate donor user exists
        User donor = donationValidationService.validateAndGetDonor(donorUserId);

        // Validate not self-donation
        donationValidationService.validateNotSelfDonation(donorUserId, campaign.getUser().getId());

        // Auto-approve: System simulates validation
        // In production, this would integrate with payment gateway
        Donation donation = Donation.builder()
                .donor(donor)
                .campaign(campaign)
                .amount(dto.getAmount())
                .status(DonationStatus.COMPLETED)
                .transactionId(generateTransactionId())
                .build();

        Donation savedDonation = donationRepository.save(donation);

        // The sum and the auto-complete are atomic UPDATEs, the last writes on the goal: its row stays
        // locked only from here to the commit. The goal in memory is stale after this and is not used again.
        LocalDateTime now = LocalDateTime.now();
        if (goalRepository.addToAmountRaised(goal.getId(), dto.getAmount(), now) == 0) {
            // The campaign was closed after the validation: the transaction rolls back the donation too
            throw new MessageException("Campaign goal is not active");
        }

        if (goalRepository.completeIfGoalReached(goal.getId(), now) == 1) {
            campaignStatusHistoryService.recordTransition(campaign, CampaignStatus.ACTIVE, CampaignStatus.COMPLETED, null);
        }

        return donationMapper.toDTO(savedDonation);
    }

    public List<DonationResponseDTO> getDonationsByAuthenticatedUser() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        List<Donation> donations = donationRepository.findByDonorIdOrderByCreatedAtDesc(userId);
        return donationMapper.toDTOList(donations, findDeletedCampaignNames(donations));
    }

    /**
     * Donations to a soft-deleted campaign have a null campaign relation. Their names are
     * fetched in a single native query that bypasses the soft-delete restriction.
     */
    private Map<Long, String> findDeletedCampaignNames(List<Donation> donations) {
        List<Long> deletedCampaignIds = donations.stream()
                .filter(donation -> donation.getCampaign() == null)
                .map(Donation::getCampaignId)
                .distinct()
                .toList();

        if (deletedCampaignIds.isEmpty()) {
            return Map.of();
        }

        return campaignRepository.findNamesByIdsIncludingDeleted(deletedCampaignIds).stream()
                .collect(Collectors.toMap(
                        CampaignRepository.CampaignNameProjection::getId,
                        CampaignRepository.CampaignNameProjection::getName));
    }

    public List<DonationResponseDTO> getDonationsByCampaign(Long campaignId) {
        Optional<Long> callerId = authenticatedUserService.findAuthenticatedUserId();
        Campaign campaign = donationValidationService.validateAndGetCampaign(campaignId, callerId.orElse(null));
        List<Donation> donations = donationRepository.findByCampaignOrderByCreatedAtDesc(campaign);
        List<DonationResponseDTO> dtos = donationMapper.toDTOList(donations);

        // The transaction id has no public use, so anonymous callers do not get it
        if (callerId.isEmpty()) {
            dtos.forEach(dto -> dto.setTransactionId(null));
        }
        return dtos;
    }

    public CampaignStatisticsDTO getCampaignStatistics(Long campaignId) {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        Campaign campaign = donationValidationService.validateAndGetCampaign(campaignId, callerId);

        BigDecimal totalRaised = donationRepository.sumCompletedDonationsByCampaignId(campaignId);
        Long uniqueDonors = donationRepository.countUniqueDonorsByCampaignId(campaignId);

        // Handle null values from queries
        totalRaised = totalRaised != null ? totalRaised : BigDecimal.ZERO;
        uniqueDonors = uniqueDonors != null ? uniqueDonors : 0L;

        Goal goal = campaign.getGoal();
        BigDecimal goalAmount = goal != null ? goal.getAmountGoal() : BigDecimal.ZERO;
        BigDecimal percentage = goalAmount.signum() > 0
                ? totalRaised.multiply(ONE_HUNDRED).divide(goalAmount, MathContext.DECIMAL64)
                : BigDecimal.ZERO;

        return CampaignStatisticsDTO.builder()
                .campaignId(campaignId)
                .campaignName(campaign.getName())
                .amountGoal(goalAmount)
                .amountRaised(totalRaised)
                .percentageReached(percentage)
                .isGoalReached(totalRaised.compareTo(goalAmount) >= 0)
                .status(goal != null ? goal.getStatus() : null)
                .totalDonors(uniqueDonors)
                .build();
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
