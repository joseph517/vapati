package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Donation;
import com.vaPaTi.vaPaTi.entity.User;
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
     * from {@code deletedCampaignNames} (campaign id to name).
     */
    public DonationResponseDTO toDTO(Donation donation, Map<Long, String> deletedCampaignNames) {
        if (donation == null) {
            return null;
        }

        User donor = donation.getDonor();
        Campaign campaign = donation.getCampaign();
        Long campaignId = campaign != null ? campaign.getId() : donation.getCampaignId();

        return DonationResponseDTO.builder()
                .id(donation.getId())
                .donorUserId(donor != null ? donor.getId() : null)
                .donorUserName(donor != null && donor.getUserInfo() != null ? donor.getUserInfo().getUserName() : null)
                .campaignId(campaignId)
                .campaignName(campaign != null ? campaign.getName() : deletedCampaignNames.get(campaignId))
                .campaignDeleted(campaign == null)
                .amount(donation.getAmount())
                .status(donation.getStatus())
                .transactionId(donation.getTransactionId())
                .createdAt(donation.getCreatedAt())
                .updatedAt(donation.getUpdatedAt())
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
