package com.vaPaTi.vaPaTi.dtos;

import lombok.Data;

@Data
public class CreateVerificationRequestDTO {
    private Long userId;
    private String dniFront;
    private String dniBack;
    private String selfieUser;
}
