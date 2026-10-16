package com.vaPaTi.vaPaTi.dtos.donation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Result of createDonation: the controller spreads it into the response body. autoThanksMessage is not stored in the donation
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationCreatedResponseDTO {

    private DonationResponseDTO donation;
    private String autoThanksMessage;
}
