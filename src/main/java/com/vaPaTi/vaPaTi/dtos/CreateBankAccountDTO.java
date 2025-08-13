package com.vaPaTi.vaPaTi.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateBankAccountDTO {
    private Long userId;
    private String bankName;
    private String accountNumber;
    private String accountType;
    private String accountHolder;
}
