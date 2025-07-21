package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Request DTO for follow/unfollow operations
public class FollowRequestDto {

    @NotNull(message = "User ID to follow is required")
    @Positive(message = "User ID must be positive")
    private Long userToFollowId;

    // Constructors
    public FollowRequestDto() {}

    public FollowRequestDto(Long userToFollowId) {
        this.userToFollowId = userToFollowId;
    }

    // Getters and setters
    public Long getUserToFollowId() {
        return userToFollowId;
    }

    public void setUserToFollowId(Long userToFollowId) {
        this.userToFollowId = userToFollowId;
    }

}
