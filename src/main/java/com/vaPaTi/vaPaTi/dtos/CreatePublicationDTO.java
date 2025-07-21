package com.vaPaTi.vaPaTi.dtos;

public class CreatePublicationDTO {

    private Long userId;
    private String description;

    // Getters y setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
