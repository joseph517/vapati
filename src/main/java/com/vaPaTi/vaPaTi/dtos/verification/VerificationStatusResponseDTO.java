package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.verification.VerificationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VerificationStatusResponseDTO {
    private boolean isVerified;
    private VerificationStatus requestStatus;
    private LocalDateTime createdAt;
}
