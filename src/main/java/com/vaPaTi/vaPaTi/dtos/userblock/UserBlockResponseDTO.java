package com.vaPaTi.vaPaTi.dtos.userblock;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

// Response DTO for a blocked user: the blocked user's basic info and when the block was created
@Data
@AllArgsConstructor
public class UserBlockResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String userName;
    private String profilePicture;
    private LocalDateTime blockedAt;
}
