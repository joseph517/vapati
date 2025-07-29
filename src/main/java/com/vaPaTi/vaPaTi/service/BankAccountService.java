package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateBankAccountDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.BankAccountMapper;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.validation.BankAccountValidationService;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import com.vaPaTi.vaPaTi.validation.AccountValidationResult;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountValidationService bankAccountValidationService;
    private final UserValidationService userValidationService;
    private final BankAccountMapper bankAccountMapper;

    public BankAccountDTO createBankAccount(@NotNull CreateBankAccountDTO dto) {

        bankAccountValidationService.validateInput(dto);
        User user = userValidationService.getUserById(dto.getUserId());
        bankAccountValidationService.verifyUserIsVerified(dto.getUserId());

        // Validate duplicate accounts for the user in active accounts
        bankAccountValidationService.checkIfUserHasDuplicateAccount(dto.getUserId(), dto.getAccountNumber());

        // Main validation of the account number
        AccountValidationResult validationResult = bankAccountValidationService
                .validateAccountCreation(dto.getAccountNumber(), dto.getUserId());

        switch (validationResult) {
            case CAN_CREATE:
                // Create new account
                BankAccount newAccount = bankAccountValidationService.buildBankAccountEntity(dto, user);
                BankAccount saved = bankAccountRepository.save(newAccount);
                return bankAccountMapper.toDto(saved);

            case CAN_RESTORE:
                // Restore deleted account
                BankAccount accountToRestore = bankAccountRepository
                        .findByUserIdAndAccountNumberAndDeletedAtIsNotNull(dto.getUserId(), dto.getAccountNumber())
                        .orElseThrow(() -> new MessageException("Account not found for restoration"));

                accountToRestore.setDeletedAt(null);
                BankAccount restored = bankAccountRepository.save(accountToRestore);
                return bankAccountMapper.toDto(restored);

            case ALREADY_EXISTS:
                throw new MessageException("Account number already exists");

            case OWNED_BY_OTHER_USER:
                throw new MessageException("Cannot create account with this number");

            default:
                throw new MessageException("Unexpected validation result");
        }
    }

    public List<BankAccountDTO> getBankAccountsByUserId(Long userId) {
        if (!userValidationService.existsById(userId)) {
            throw new MessageException("User not found with id: " + userId);
        }

        List<BankAccount> bankAccounts = bankAccountRepository.findByUserId(userId);

        if (bankAccounts.isEmpty()) {
            throw new MessageException("No bank accounts found for user with id: " + userId);
        }

        return bankAccounts.stream()
                .map(bankAccountMapper::toDto)
                .toList();
    }

    @Transactional
    public void deleteBankAccount(Long id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new MessageException("Bank account not found with id: " + id));

        if (bankAccount.getDeletedAt() != null) {
            throw new MessageException("Bank account is already deleted");
        }

        bankAccount.setDeletedAt(LocalDateTime.now());
        bankAccountRepository.save(bankAccount);
    }

    @Transactional
    public BankAccountDTO updateBankAccount(Long id, UpdateBankAccountDTO dto) {
        // Validate input
        bankAccountValidationService.validateUpdateInput(dto);

        // Search for the bank account
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new MessageException("Bank account not found with id: " + id));

        // Verify that the account is not deleted
        if (bankAccount.getDeletedAt() != null) {
            throw new MessageException("Cannot update deleted bank account");
        }

        // Validate and update fields only if they are sent and not empty
        if (dto.getBankName() != null) {
            if (dto.getBankName().trim().isEmpty()) {
                throw new MessageException("Bank name cannot be empty");
            }
            bankAccount.setBankName(dto.getBankName().trim());
        }

        if (dto.getAccountNumber() != null) {
            bankAccountValidationService.validateAccountNumberForUpdate(
                    dto.getAccountNumber(),
                    bankAccount.getAccountNumber(),
                    bankAccount.getUser().getId()
            );
            bankAccount.setAccountNumber(dto.getAccountNumber().trim());
        }

        if (dto.getAccountType() != null) {
            if (dto.getAccountType().trim().isEmpty()) {
                throw new MessageException("Account type cannot be empty");
            }
            bankAccount.setAccountType(dto.getAccountType().trim());
        }

        if (dto.getAccountHolder() != null) {
            if (dto.getAccountHolder().trim().isEmpty()) {
                throw new MessageException("Account holder cannot be empty");
            }
            bankAccount.setAccountHolder(dto.getAccountHolder().trim());
        }

        BankAccount updated = bankAccountRepository.save(bankAccount);

        return bankAccountMapper.toDto(updated);
    }
}
