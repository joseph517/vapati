package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Partial update: a null name keeps the current one, but a sent name can't be blank
@Data
public class UpdateCategoryDTO {
    @Size(max = 255)
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    private String name;

    @Size(max = 255)
    private String description;
}
