package com.vaPaTi.vaPaTi.dtos.donation;

import com.vaPaTi.vaPaTi.entity.donation.DonationCtaText;
import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// A creator's donation panel, the same for the owner and for everyone else. suggestedAmounts is never null
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationSettingsResponseDTO {

    private Long userId;
    private DonationMode mode;
    private String unitName;
    private BigDecimal unitAmount;
    private BigDecimal minimumAmount;
    private BigDecimal effectiveMinimumAmount;
    private List<BigDecimal> suggestedAmounts;
    private DonationCtaText ctaText;
}
