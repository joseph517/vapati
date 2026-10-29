package com.vaPaTi.vaPaTi.dtos.userblock;

import lombok.AllArgsConstructor;
import lombok.Data;

// Response DTO for unblock operation
@Data
@AllArgsConstructor
public class UnblockResponseDTO {
    private String message;
    private boolean success;
    private Long unblockedUserId;
}
