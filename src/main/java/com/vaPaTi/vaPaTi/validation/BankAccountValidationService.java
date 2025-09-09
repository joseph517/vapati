package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateBankAccountDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.vaPaTi.vaPaTi.utils.ValidationUtils.*;

@Service
@RequiredArgsConstructor
public class BankAccountValidationService {

    private static final String USER_NOT_FOUND = "User not found";
    private static final String USER_NOT_VERIFIED = "User is not verified";
    private final BankAccountRepository bankAccountRepository;

    public void validateInput(@NotNull CreateBankAccountDTO dto) {
        validateNotBlank(dto.getBankName(), "Bank name");
        validateNotBlank(dto.getAccountHolder(), "Account holder");
        validateNotBlank(dto.getAccountNumber(), "Account number");
        validateNotBlank(dto.getAccountType(), "Account type");
        if (dto.getUserId() == null) {
            throw new MessageException("User ID is required");
        }
    }

    public void validateUpdateInput(UpdateBankAccountDTO dto) {
        if (dto == null) {
            throw new MessageException("Update data cannot be null");
        }

        validateAtLeastOneFieldPresent(
                dto.getBankName(),
                dto.getAccountNumber(),
                dto.getAccountType(),
                dto.getAccountHolder()
        );

        validateIfPresent(dto.getBankName(), "Bank name");
        validateIfPresent(dto.getAccountNumber(), "Account number");
        validateIfPresent(dto.getAccountType(), "Account type");
        validateIfPresent(dto.getAccountHolder(), "Account holder");
    }

    public void verifyUserIsVerified(Long userId) {
        Boolean isVerified = bankAccountRepository.isUserVerified(userId)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));
        if (!isVerified) {
            throw new MessageException(USER_NOT_VERIFIED);
        }
    }

    public AccountValidationResult validateAccountCreation(String accountNumber, Long userId) {
        Optional<BankAccount> existingAccount = bankAccountRepository.findByAccountNumberIgnoreDeleted(accountNumber);

        if (existingAccount.isEmpty()) {
            return AccountValidationResult.CAN_CREATE;
        }

        BankAccount account = existingAccount.get();
        boolean isActive = account.getDeletedAt() == null;
        boolean isOwner = account.getUser().getId().equals(userId);

        if (isActive) {
            return AccountValidationResult.ALREADY_EXISTS;
        }

        if (!isOwner) {
            return AccountValidationResult.OWNED_BY_OTHER_USER;
        }

        return AccountValidationResult.CAN_RESTORE;
    }

    public void checkIfUserHasDuplicateAccount(Long userId, String accountNumber) {
        if (bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, accountNumber)) {
            throw new MessageException("User already has a bank account with this account number");
        }
    }

    public void validateAccountNumberForUpdate(String newAccountNumber, String currentAccountNumber, Long userId) {
        if (newAccountNumber == null || newAccountNumber.trim().isEmpty()) {
            throw new MessageException("Account number cannot be empty");
        }

        String trimmedNewAccountNumber = newAccountNumber.trim();

        if (trimmedNewAccountNumber.equals(currentAccountNumber)) {
            return;
        }

        if (bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(trimmedNewAccountNumber)) {
            throw new MessageException("Account number already exists");
        }

        Optional<BankAccount> existingAccount = bankAccountRepository.findByAccountNumberIgnoreDeleted(trimmedNewAccountNumber);
        if (existingAccount.isPresent()) {
            BankAccount existing = existingAccount.get();
            if (!existing.getUser().getId().equals(userId)) {
                throw new MessageException("Cannot use account number owned by another user");
            }
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

}
