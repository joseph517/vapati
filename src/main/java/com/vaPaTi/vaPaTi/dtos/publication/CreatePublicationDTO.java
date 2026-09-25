package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePublicationDTO {
    @NotBlank
    @Size(max = 255)
    private String description;
}
