package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.validation.DonationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationResponseDTO {

    private Long id;
    private Long donorUserId;
    private String donorUserName;
    private Long campaignId;
    private String campaignName;
    private Double amount;
    private DonationStatus status;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
