package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class BankAccountValidationService {

    private static final String USER_NOT_FOUND = "User not found";
    private static final String USER_NOT_VERIFIED = "User is not verified";

    private final BankAccountRepository bankAccountRepository;

    // Constructor
    public BankAccountValidationService(
            BankAccountRepository bankAccountRepository
    ) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public void validateInput(@NotNull CreateBankAccountDTO dto) {
        if (dto.getBankName() == null || dto.getBankName().isBlank()) {
            throw new MessageException("Bank name is required");
        }
        if (dto.getAccountHolder() == null || dto.getAccountHolder().isBlank()) {
            throw new MessageException("Account holder is required");
        }
        if (dto.getAccountNumber() == null || dto.getAccountNumber().isBlank()) {
            throw new MessageException("Account number is required");
        }
        if (dto.getAccountType() == null || dto.getAccountType().isBlank()) {
            throw new MessageException("Account type is required");
        }
        if (dto.getUserId() == null) {
            throw new MessageException("User ID is required");
        }
    }

    public void verifyUserIsVerified(Long userId) {
        Boolean isVerified = bankAccountRepository.isUserVerified(userId)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));
        if (!isVerified) {
            throw new MessageException(USER_NOT_VERIFIED);
        }
    }

    public void checkIfAccountNumberExists(String accountNumber) {
        if (bankAccountRepository.existsByAccountNumber(accountNumber)) {
            throw new MessageException("Account number already exists");
        }
    }

    public void checkIfUserHasDuplicateAccount(Long userId, String accountNumber) {
        if (bankAccountRepository.existsByUserIdAndAccountNumber(userId, accountNumber)) {
            throw new MessageException("User already has a bank account with this account number");
        }
    }

    public @NotNull BankAccount buildBankAccountEntity(@NotNull CreateBankAccountDTO dto, User user) {
        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setBankName(dto.getBankName());
        account.setAccountNumber(dto.getAccountNumber());
        account.setAccountType(dto.getAccountType());
        account.setAccountHolder(dto.getAccountHolder());
        return account;
    }

    public @NotNull BankAccountDTO convertToDTO(@NotNull BankAccount bankAccount) {
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
