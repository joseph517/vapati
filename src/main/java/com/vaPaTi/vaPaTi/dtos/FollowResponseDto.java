package com.vaPaTi.vaPaTi.dtos;

// Response DTO for follow operation
public class FollowResponseDto {

    private String message;
    private boolean success;
    private FollowerUserDto followedUser;

    public FollowResponseDto(String message, boolean success, FollowerUserDto followedUser) {
        this.message = message;
        this.success = success;
        this.followedUser = followedUser;
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

    public FollowerUserDto getFollowedUser() {
        return followedUser;
    }

    public void setFollowedUser(FollowerUserDto followedUser) {
        this.followedUser = followedUser;
    }
}
