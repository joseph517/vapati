package com.vaPaTi.vaPaTi.dtos;

import lombok.Data;

import java.time.LocalDateTime;
@Data
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
