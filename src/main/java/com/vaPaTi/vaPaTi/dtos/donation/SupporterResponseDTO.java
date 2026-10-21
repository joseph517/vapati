package com.vaPaTi.vaPaTi.dtos.donation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupporterResponseDTO {

    private Long donorUserId;             // null if the donor is soft deleted
    private String donorUserName;         // null if the donor is soft deleted
    private BigDecimal totalAmount;       // null unless the caller is the creator or an ADMIN
    private Long donationsCount;          // idem
    private LocalDateTime lastDonationAt; // idem
}
