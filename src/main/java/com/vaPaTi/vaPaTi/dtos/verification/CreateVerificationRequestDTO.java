package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVerificationRequestDTO {
    private Long userId;
    @NotBlank
    @Size(max = 255)
    private String dniFront;
    @NotBlank
    @Size(max = 255)
    private String dniBack;
    @NotBlank
    @Size(max = 255)
    private String selfieUser;
}
