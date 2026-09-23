package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CreateBankAccountDTO {
    private Long userId;
    @NotBlank
    @Size(max = 100)
    private String bankName;
    @NotBlank
    @Size(max = 50)
    private String accountNumber;
    @NotBlank
    @Size(max = 50)
    private String accountType;
    @NotBlank
    @Size(max = 100)
    private String accountHolder;
}
