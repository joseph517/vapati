package com.vaPaTi.vaPaTi.dtos.donation;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDonationDTO {

    private Long campaignId;

    private Long recipientUserId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal amount;

    @Size(max = 280, message = "Message must be at most 280 characters")
    private String message;

    private Boolean messagePrivate;
}
