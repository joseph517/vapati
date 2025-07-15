package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.BankAccountMapper;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.validation.BankAccountValidationService;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import com.vaPaTi.vaPaTi.validation.AccountValidationResult;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountValidationService bankAccountValidationService;
    private final UserValidationService userValidationService;
    private final BankAccountMapper bankAccountMapper;

    // Constructor
    public BankAccountService(
            BankAccountRepository bankAccountRepository,
            BankAccountValidationService bankAccountValidationService,
            UserValidationService userValidationService,
            BankAccountMapper bankAccountMapper
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankAccountValidationService = bankAccountValidationService;
        this.userValidationService = userValidationService;
        this.bankAccountMapper = bankAccountMapper;
    }

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
}
