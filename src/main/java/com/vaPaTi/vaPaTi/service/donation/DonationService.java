package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CampaignStatisticsDTO;
import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationThanksDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationCreatedResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.donation.DonationStatus;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationMapper;
import com.vaPaTi.vaPaTi.repository.campaign.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.campaign.GoalRepository;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.campaign.CampaignStatusHistoryService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.donation.DonationValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final CampaignAuthorizationService campaignAuthorizationService;

    @Transactional
    public DonationCreatedResponseDTO createDonation(@NotNull CreateDonationDTO dto) {
        // Get authenticated user
        Long donorUserId = authenticatedUserService.getAuthenticatedUserId();

        // Validate input
        donationValidationService.validateInput(dto);

        // Normalize the optional message. A private flag without a message has nothing to hide
        String message = donationValidationService.validateAndNormalizeMessage(dto.getMessage());
        boolean messagePrivate = message != null && Boolean.TRUE.equals(dto.getMessagePrivate());

        // validateInput guarantees exactly one destination: a campaign or a user (direct donation)
        Campaign campaign = null;
        User recipient;
        User donor;
        if (dto.getCampaignId() != null) {
            // Validate campaign exists, is visible to the donor and is not deleted (before the status check)
            campaign = donationValidationService.validateAndGetCampaign(dto.getCampaignId(), donorUserId);
            donationValidationService.validateGoalIsActive(campaign.getGoal());
            donor = donationValidationService.validateAndGetDonor(donorUserId);
            donationValidationService.validateNotSelfDonation(donorUserId, campaign.getUser().getId());
            // Never null: campaigns of deleted owners are not visible
            recipient = campaign.getUser();
        } else {
            // A direct donation: no campaign, no goal and no status history
            recipient = donationValidationService.validateAndGetRecipient(dto.getRecipientUserId());
            donor = donationValidationService.validateAndGetDonor(donorUserId);
            donationValidationService.validateNotSelfDirectDonation(donorUserId, recipient.getId());
        }

        // Auto-approve: System simulates validation
        // In production, this would integrate with payment gateway
        Donation donation = Donation.builder()
                .donor(donor)
                .recipient(recipient)
                .campaign(campaign)
                .amount(dto.getAmount())
                .status(DonationStatus.COMPLETED)
                .transactionId(generateTransactionId())
                .message(message)
                .messagePrivate(messagePrivate)
                .build();

        Donation savedDonation = donationRepository.save(donation);

        if (campaign != null) {
            addToCampaignGoal(campaign, dto.getAmount());
        }

        // The recipient's current default thanks travels only in this response, it is not copied into the donation
        return DonationCreatedResponseDTO.builder()
                .donation(donationMapper.toDTO(savedDonation))
                .autoThanksMessage(findAutoThanksMessage(recipient))
                .build();
    }

    // The sum and the auto-complete are atomic UPDATEs, the last writes on the goal: its row stays
    // locked only from here to the commit. The goal in memory is stale after this and is not used again.
    private void addToCampaignGoal(Campaign campaign, BigDecimal amount) {
        Goal goal = campaign.getGoal();
        LocalDateTime now = LocalDateTime.now();
        if (goalRepository.addToAmountRaised(goal.getId(), amount, now) == 0) {
            // The campaign was closed after the validation: the transaction rolls back the donation too
            throw new MessageException("Campaign goal is not active");
        }

        if (goalRepository.completeIfGoalReached(goal.getId(), now) == 1) {
            campaignStatusHistoryService.recordTransition(campaign, CampaignStatus.ACTIVE, CampaignStatus.COMPLETED, null);
        }
    }

    private String findAutoThanksMessage(User recipient) {
        UserInfo recipientInfo = recipient != null ? recipient.getUserInfo() : null;
        return recipientInfo != null ? recipientInfo.getAutoThanksMessage() : null;
    }

    // One reply per donation, only by the campaign owner. It is not editable: it has to be deleted to reply again
    @Transactional
    public DonationResponseDTO thankDonation(Long donationId, @NotNull CreateDonationThanksDTO dto) {
        Long callerId = authenticatedUserService.getAuthenticatedUserId();

        String thanksMessage = donationValidationService.validateAndNormalizeThanksMessage(dto.getThanksMessage());

        Donation donation = donationValidationService.validateAndGetDonation(donationId);
        Campaign campaign = donationValidationService.validateAndGetDonationCampaign(donation);
        donationValidationService.validateIsCampaignOwner(campaign, callerId);
        donationValidationService.validateDonationCompleted(donation);
        donationValidationService.validateHasNoThanks(donation);

        donation.setThanksMessage(thanksMessage);
        donation.setThanksPrivate(Boolean.TRUE.equals(dto.getThanksPrivate()));
        donation.setThankedAt(LocalDateTime.now());

        return donationMapper.toDTO(donationRepository.save(donation));
    }

    // The campaign owner or an ADMIN. Clears the three fields together, so the owner can reply again
    @Transactional
    public DonationResponseDTO deleteDonationThanks(Long donationId) {
        Long callerId = authenticatedUserService.getAuthenticatedUserId();

        Donation donation = donationValidationService.validateAndGetDonation(donationId);
        Campaign campaign = donationValidationService.validateAndGetDonationCampaign(donation);
        donationValidationService.validateIsCampaignOwnerOrAdmin(campaign, callerId);
        donationValidationService.validateHasThanks(donation);

        donation.setThanksMessage(null);
        donation.setThanksPrivate(false);
        donation.setThankedAt(null);

        return donationMapper.toDTO(donationRepository.save(donation));
    }

    @Transactional
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
                .filter(donation -> donation.getCampaignId() != null && donation.getCampaign() == null)
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

    @Transactional
    public List<DonationResponseDTO> getDonationsByCampaign(Long campaignId) {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        Campaign campaign = donationValidationService.validateAndGetCampaign(campaignId, callerId);
        List<Donation> donations = donationRepository.findByCampaignOrderByCreatedAtDesc(campaign);
        List<DonationResponseDTO> dtos = donationMapper.toDTOList(donations);

        // The transaction id (the payment reference), a private message and a private reply are seen by the
        // campaign owner and admins on every donation, by a donor on their own, and by anyone else (anonymous
        // included) on none. A hidden private message or reply looks like no message or reply at all, without
        // thankedAt either. The donor stays public.
        if (!canSeePrivateDonationData(campaign, callerId)) {
            dtos.stream()
                    .filter(dto -> callerId == null || !callerId.equals(dto.getDonorUserId()))
                    .forEach(dto -> {
                        dto.setTransactionId(null);
                        if (Boolean.TRUE.equals(dto.getMessagePrivate())) {
                            dto.setMessage(null);
                            dto.setMessagePrivate(false);
                        }
                        if (Boolean.TRUE.equals(dto.getThanksPrivate())) {
                            dto.setThanksMessage(null);
                            dto.setThanksPrivate(false);
                            dto.setThankedAt(null);
                        }
                    });
        }
        return dtos;
    }

    private boolean canSeePrivateDonationData(Campaign campaign, Long callerId) {
        if (callerId == null) {
            return false;
        }
        boolean isOwner = campaign.getUser() != null && callerId.equals(campaign.getUser().getId());
        return isOwner || campaignAuthorizationService.isAdmin(callerId);
    }

    public CampaignStatisticsDTO getCampaignStatistics(Long campaignId) {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        Campaign campaign = donationValidationService.validateAndGetCampaign(campaignId, callerId);

        Long totalDonations = donationRepository.countCompletedDonationsByCampaignId(campaignId);
        Long uniqueDonors = donationRepository.countUniqueDonorsByCampaignId(campaignId);

        // Handle null values from queries
        totalDonations = totalDonations != null ? totalDonations : 0L;
        uniqueDonors = uniqueDonors != null ? uniqueDonors : 0L;

        // amountRaised is the campaign's own total, the same value the campaign returns
        Goal goal = campaign.getGoal();
        BigDecimal amountGoal = goal != null ? goal.getAmountGoal() : BigDecimal.ZERO;
        BigDecimal amountRaised = goal != null ? goal.getAmountRaised() : BigDecimal.ZERO;

        // A goal <= 0 only exists in dev test data (P24): 0% instead of dividing by zero
        BigDecimal percentage = amountGoal.signum() > 0
                ? amountRaised.multiply(ONE_HUNDRED).divide(amountGoal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal averageDonation = totalDonations > 0
                ? amountRaised.divide(BigDecimal.valueOf(totalDonations), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return CampaignStatisticsDTO.builder()
                .campaignId(campaignId)
                .campaignName(campaign.getName())
                .amountGoal(amountGoal)
                .amountRaised(amountRaised)
                .percentageReached(percentage)
                .isGoalReached(amountRaised.compareTo(amountGoal) >= 0)
                .status(goal != null ? goal.getStatus() : null)
                .totalDonors(uniqueDonors)
                .totalDonations(totalDonations)
                .averageDonation(averageDonation)
                .build();
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
