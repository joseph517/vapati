package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBankAccountDTO {

    @Size(max = 100, message = "Bank name cannot exceed 100 characters")
    private String bankName;

    @Size(max = 50, message = "Account number cannot exceed 50 characters")
    private String accountNumber;

    @Size(max = 50, message = "Account type cannot exceed 50 characters")
    private String accountType;

    @Size(max = 100, message = "Account holder cannot exceed 100 characters")
    private String accountHolder;
}