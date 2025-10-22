package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateDonationDTO {

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private Double amount;
}
