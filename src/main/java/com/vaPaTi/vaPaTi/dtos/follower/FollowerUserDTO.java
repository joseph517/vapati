package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

// Response DTO for displaying user basic info in follower lists
@Data
@AllArgsConstructor
public class FollowerUserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String userName;
    private String profilePicture;
    private LocalDateTime followedAt; // When this follow relationship was created
}
