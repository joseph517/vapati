package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.validation.VerificationStatus;
import lombok.Data;

@Data
public class ProcessVerificationRequestDTO {
    private Long requestId;
    private VerificationStatus status;
}
