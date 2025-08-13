package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDTO {
    private Long id;
    private Long userId;
    private String bankName;
    private String accountNumber;
    private String accountType;
    private String accountHolder;
}
