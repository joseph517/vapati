package com.vaPaTi.vaPaTi.dtos.follower;

import lombok.AllArgsConstructor;
import lombok.Data;

// Response DTO for unfollow operation
@Data
@AllArgsConstructor
public class UnfollowResponseDTO {
    private String message;
    private boolean success;
    private Long unfollowedUserId;
}
