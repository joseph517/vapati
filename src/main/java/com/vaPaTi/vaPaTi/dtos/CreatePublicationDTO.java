package com.vaPaTi.vaPaTi.dtos;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePublicationDTO {
    private Long userId;
    private String description;
}
