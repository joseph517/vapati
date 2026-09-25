package com.vaPaTi.vaPaTi.mapper.bankaccount;

import com.vaPaTi.vaPaTi.dtos.bankaccount.BankAccountDTO;
import com.vaPaTi.vaPaTi.entity.bankaccount.BankAccount;
import org.springframework.stereotype.Component;

@Component
public class BankAccountMapper {

    public BankAccountDTO toDto(BankAccount bankAccount) {
        if (bankAccount == null) {
            return null;
        }

        BankAccountDTO dto = new BankAccountDTO();
        dto.setId(bankAccount.getId());
        dto.setUserId(bankAccount.getUser().getId());
        dto.setBankName(bankAccount.getBankName());
        dto.setAccountNumber(bankAccount.getAccountNumber());
        dto.setAccountType(bankAccount.getAccountType());
        dto.setAccountHolder(bankAccount.getAccountHolder());
        return dto;
    }
}