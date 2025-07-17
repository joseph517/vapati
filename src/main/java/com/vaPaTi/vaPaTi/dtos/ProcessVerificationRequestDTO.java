package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.validation.VerificationStatus;

public class ProcessVerificationRequestDTO {

    private Long requestId;
    private VerificationStatus status;

    // Getters y setters

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }
}
