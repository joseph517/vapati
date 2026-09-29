package com.vaPaTi.vaPaTi.dtos.verification;

import com.vaPaTi.vaPaTi.entity.verification.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProcessVerificationRequestDTO {
    @NotNull
    @Positive
    private Long requestId;
    @NotNull
    private VerificationStatus status;
}
