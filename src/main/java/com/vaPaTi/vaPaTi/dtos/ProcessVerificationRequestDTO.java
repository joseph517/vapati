package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.validation.VerificationStatus;
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
