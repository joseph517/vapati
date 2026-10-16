package com.vaPaTi.vaPaTi.dtos.donation;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDonationThanksDTO {

    @Size(max = 280, message = "Thanks message must be at most 280 characters")
    private String thanksMessage;

    private Boolean thanksPrivate;
}
