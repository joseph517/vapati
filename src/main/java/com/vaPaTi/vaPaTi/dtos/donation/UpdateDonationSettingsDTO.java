package com.vaPaTi.vaPaTi.dtos.donation;

import com.vaPaTi.vaPaTi.entity.donation.DonationCtaText;
import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// Replaces the whole donation panel configuration. Unit fields are shape-checked in every mode; UNIT rules live in DonationSettingsValidationService
@Data
public class UpdateDonationSettingsDTO {

    @NotNull(message = "Mode is required")
    private DonationMode mode;

    @Size(max = 30, message = "Unit name must be at most 30 characters")
    private String unitName;

    @Positive(message = "Unit amount must be greater than zero")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal unitAmount;

    // null = no minimum
    @Positive(message = "Minimum amount must be greater than zero")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal minimumAmount;

    // null or [] = no suggested amounts
    @Size(max = 3, message = "At most 3 suggested amounts are allowed")
    private List<
            @NotNull(message = "Suggested amount is required")
            @Positive(message = "Suggested amount must be greater than zero")
            @Digits(integer = 13, fraction = 2)
                    BigDecimal> suggestedAmounts;

    @NotNull(message = "CTA text is required")
    private DonationCtaText ctaText;
}
