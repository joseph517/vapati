package com.vaPaTi.vaPaTi.dtos;

// Response DTO for unfollow operation
public class UnfollowResponseDto {

    private String message;
    private boolean success;
    private Long unfollowedUserId;

    public UnfollowResponseDto(String message, boolean success, Long unfollowedUserId) {
        this.message = message;
        this.success = success;
        this.unfollowedUserId = unfollowedUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getUnfollowedUserId() {
        return unfollowedUserId;
    }

    public void setUnfollowedUserId(Long unfollowedUserId) {
        this.unfollowedUserId = unfollowedUserId;
    }
}
