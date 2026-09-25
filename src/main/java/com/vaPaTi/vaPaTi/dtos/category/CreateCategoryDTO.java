package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryDTO {
    @NotBlank
    @Size(max = 255)
    private String name;
    @Size(max = 255)
    private String description;
}
