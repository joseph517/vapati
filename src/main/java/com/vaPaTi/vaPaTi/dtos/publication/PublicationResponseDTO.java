package com.vaPaTi.vaPaTi.dtos.publication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicationResponseDTO {
    private Long id;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long campaignId;

    private Long userId;
    private String firstName;
    private String lastName;
    private String userName;

    private Long likesCount;     // likes of active users that are not banned or suspended
    private Boolean likedByMe;   // false for an anonymous caller
}
