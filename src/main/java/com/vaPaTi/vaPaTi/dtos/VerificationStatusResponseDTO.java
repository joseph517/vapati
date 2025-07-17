package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.validation.VerificationStatus;

import java.time.LocalDateTime;

public class VerificationStatusResponseDTO {

    private boolean isVerified;
    private VerificationStatus requestStatus;
    private LocalDateTime createdAt;

    // Getters y setters

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public VerificationStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(VerificationStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
