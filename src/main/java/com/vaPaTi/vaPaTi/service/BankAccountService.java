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
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

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

    // Create bank account
    public BankAccountDTO createBankAccount(@NotNull CreateBankAccountDTO dto) {
        bankAccountValidationService.validateInput(dto);
        User user = userValidationService.getUserById(dto.getUserId());
        bankAccountValidationService.verifyUserIsVerified(dto.getUserId());
        bankAccountValidationService.checkIfAccountNumberExists(dto.getAccountNumber());
        bankAccountValidationService.checkIfUserHasDuplicateAccount(dto.getUserId(), dto.getAccountNumber());
        BankAccount bankAccount = bankAccountValidationService.buildBankAccountEntity(dto, user);
        BankAccount saved = bankAccountRepository.save(bankAccount);
        return bankAccountMapper.toDto(saved);
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
}
