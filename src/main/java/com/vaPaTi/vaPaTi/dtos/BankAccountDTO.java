package com.vaPaTi.vaPaTi.dtos;

import lombok.Data;

@Data
public class BankAccountDTO {
    private Long id;
    private Long userId;
    private String bankName;
    private String accountNumber;
    private String accountType;
    private String accountHolder;
}
