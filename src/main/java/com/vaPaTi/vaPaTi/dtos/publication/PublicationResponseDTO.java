package com.vaPaTi.vaPaTi.dtos;

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

    private Long userId;
    private String firstName;
    private String lastName;
    private String userName;
}
