package com.vaPaTi.vaPaTi.dtos.follower;

import lombok.AllArgsConstructor;
import lombok.Data;

// Response DTO for follow operation
@Data
@AllArgsConstructor
public class FollowResponseDTO {
    private String message;
    private boolean success;
    private FollowerUserDTO followedUser;
}
