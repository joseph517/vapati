package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Donation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DonationMapper {

    public DonationResponseDTO toDTO(Donation donation) {
        if (donation == null) {
            return null;
        }

        return DonationResponseDTO.builder()
                .id(donation.getId())
                .donorUserId(donation.getDonor().getId())
                .donorUserName(donation.getDonor().getUserInfo().getUserName())
                .campaignId(donation.getCampaign().getId())
                .campaignName(donation.getCampaign().getName())
                .amount(donation.getAmount())
                .status(donation.getStatus())
                .transactionId(donation.getTransactionId())
                .createdAt(donation.getCreatedAt())
                .updatedAt(donation.getUpdatedAt())
                .build();
    }

    public List<DonationResponseDTO> toDTOList(List<Donation> donations) {
        if (donations == null) {
            return List.of();
        }

        return donations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
