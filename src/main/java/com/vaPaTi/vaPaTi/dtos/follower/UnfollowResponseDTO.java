package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

// Response DTO for unfollow operation
@Data
@AllArgsConstructor
public class UnfollowResponseDto {
    private String message;
    private boolean success;
    private Long unfollowedUserId;
}
