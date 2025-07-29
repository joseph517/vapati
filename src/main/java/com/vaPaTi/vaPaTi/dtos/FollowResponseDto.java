package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

// Response DTO for follow operation
@Data
@AllArgsConstructor
public class FollowResponseDto {
    private String message;
    private boolean success;
    private FollowerUserDto followedUser;
}
