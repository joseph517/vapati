package com.vaPaTi.vaPaTi.dtos;

public class CreateVerificationRequestDTO {

    private Long userId;
    private String dniFront;
    private String dniBack;
    private String selfieUser;

    // Getters y setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDniFront() {
        return dniFront;
    }

    public void setDniFront(String dniFront) {
        this.dniFront = dniFront;
    }

    public String getDniBack() {
        return dniBack;
    }

    public void setDniBack(String dniBack) {
        this.dniBack = dniBack;
    }

    public String getSelfieUser() {
        return selfieUser;
    }

    public void setSelfieUser(String selfieUser) {
        this.selfieUser = selfieUser;
    }
}
