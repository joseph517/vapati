package com.vaPaTi.vaPaTi.mapper.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.SupporterResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DonationMapper {

    public DonationResponseDTO toDTO(Donation donation) {
        return toDTO(donation, Map.of());
    }

    /**
     * Maps a donation tolerating a soft-deleted donor or campaign (the relation is null).
     * A deleted donor is anonymous. A deleted campaign keeps its id and takes its name
     * from {@code deletedCampaignNames} (campaign id to name). A direct donation has no campaign
     * ({@code campaignId} null, {@code campaignDeleted} false). A deleted recipient has null recipient fields.
     */
    public DonationResponseDTO toDTO(Donation donation, Map<Long, String> deletedCampaignNames) {
        if (donation == null) {
            return null;
        }

        User donor = donation.getDonor();
        User recipient = donation.getRecipient();
        Campaign campaign = donation.getCampaign();
        Long campaignId = campaign != null ? campaign.getId() : donation.getCampaignId();

        return DonationResponseDTO.builder()
                .id(donation.getId())
                .donorUserId(donor != null ? donor.getId() : null)
                .donorUserName(donor != null && donor.getUserInfo() != null ? donor.getUserInfo().getUserName() : null)
                .recipientUserId(recipient != null ? recipient.getId() : null)
                .recipientUserName(recipient != null && recipient.getUserInfo() != null ? recipient.getUserInfo().getUserName() : null)
                .campaignId(campaignId)
                .campaignName(resolveCampaignName(campaign, campaignId, deletedCampaignNames))
                .campaignDeleted(campaignId != null && campaign == null)
                .amount(donation.getAmount())
                .status(donation.getStatus())
                .transactionId(donation.getTransactionId())
                .message(donation.getMessage())
                .messagePrivate(donation.isMessagePrivate())
                .thanksMessage(donation.getThanksMessage())
                .thanksPrivate(donation.isThanksPrivate())
                .thankedAt(donation.getThankedAt())
                .createdAt(donation.getCreatedAt())
                .updatedAt(donation.getUpdatedAt())
                .build();
    }

    // A live campaign gives its own name, a deleted one the name looked up by id, a direct donation none
    private String resolveCampaignName(Campaign campaign, Long campaignId, Map<Long, String> deletedCampaignNames) {
        if (campaign != null) {
            return campaign.getName();
        }

        return campaignId != null ? deletedCampaignNames.get(campaignId) : null;
    }

    /**
     * Maps a supporter row (the donor's aggregates over the creator's COMPLETED donations). A soft-deleted
     * donor ({@code donor == null}) is anonymous, like in {@link #toDTO(Donation, Map)}. The visibility of the
     * aggregates is decided by the service, not here.
     */
    public SupporterResponseDTO toSupporterDTO(DonationRepository.SupporterProjection row, User donor) {
        if (row == null) {
            return null;
        }

        return SupporterResponseDTO.builder()
                .donorUserId(donor != null ? donor.getId() : null)
                .donorUserName(donor != null && donor.getUserInfo() != null ? donor.getUserInfo().getUserName() : null)
                .totalAmount(row.getTotalAmount())
                .donationsCount(row.getDonationsCount())
                .lastDonationAt(row.getLastDonationAt())
                .build();
    }

    public List<DonationResponseDTO> toDTOList(List<Donation> donations) {
        return toDTOList(donations, Map.of());
    }

    public List<DonationResponseDTO> toDTOList(List<Donation> donations, Map<Long, String> deletedCampaignNames) {
        if (donations == null) {
            return List.of();
        }

        return donations.stream()
                .map(donation -> toDTO(donation, deletedCampaignNames))
                .collect(Collectors.toList());
    }
}
