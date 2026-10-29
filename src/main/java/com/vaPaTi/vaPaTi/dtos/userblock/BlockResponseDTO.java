package com.vaPaTi.vaPaTi.dtos.userblock;

import lombok.AllArgsConstructor;
import lombok.Data;

// Response DTO for block operation
@Data
@AllArgsConstructor
public class BlockResponseDTO {
    private String message;
    private boolean success;
    private UserBlockResponseDTO blockedUser;
}
