package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request DTO for follow/unfollow operations
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowRequestDto {
    @NotNull(message = "User ID to follow is required")
    @Positive(message = "User ID must be positive")
    private Long userToFollowId;
}
