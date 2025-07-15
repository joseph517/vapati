package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateBankAccountDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.BankAccountMapper;
import com.vaPaTi.vaPaTi.mapper.UserMapper;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BankAccountValidationService {

    private static final String USER_NOT_FOUND = "User not found";
    private static final String USER_NOT_VERIFIED = "User is not verified";

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountMapper bankAccountMapper;

    // Constructor
    public BankAccountValidationService(
            BankAccountRepository bankAccountRepository,
            BankAccountMapper bankAccountMapper
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankAccountMapper = bankAccountMapper;
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

    @Transactional
    public BankAccountDTO updateBankAccount(Long id, UpdateBankAccountDTO dto) {
        // Validar input
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new MessageException("Bank account not found with id: " + id));

        // Verify that the account is not deleted
        if (bankAccount.getDeletedAt() != null) {
            throw new MessageException("Cannot update deleted bank account");
        }

        // Validate and update fields only if sent and not empty
        if (dto.getBankName() != null) {
            if (dto.getBankName().trim().isEmpty()) {
                throw new MessageException("Bank name cannot be empty");
            }
            bankAccount.setBankName(dto.getBankName().trim());
        }

        if (dto.getAccountNumber() != null) {
            if (dto.getAccountNumber().trim().isEmpty()) {
                throw new MessageException("Account number cannot be empty");
            }

            // Validate that the new account number is not in use by another active account
            String newAccountNumber = dto.getAccountNumber().trim();
            if (!newAccountNumber.equals(bankAccount.getAccountNumber())) {
                // Verificar que no exista otra cuenta activa con este número
                if (bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(newAccountNumber)) {
                    throw new MessageException("Account number already exists");
                }

                // Verify that it is not owned by another user (including deleted accounts)
                Optional<BankAccount> existingAccount = bankAccountRepository.findByAccountNumberIgnoreDeleted(newAccountNumber);
                if (existingAccount.isPresent()) {
                    BankAccount existing = existingAccount.get();
                    if (!existing.getUser().getId().equals(bankAccount.getUser().getId())) {
                        throw new MessageException("Cannot use account number owned by another user");
                    }
                }
            }
            bankAccount.setAccountNumber(newAccountNumber);
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

    public void validateUpdateInput(UpdateBankAccountDTO dto) {
        if (dto == null) {
            throw new MessageException("Update data cannot be null");
        }

        // Validar que al menos un campo se esté enviando para actualizar
        if (dto.getBankName() == null &&
                dto.getAccountNumber() == null &&
                dto.getAccountType() == null &&
                dto.getAccountHolder() == null) {
            throw new MessageException("At least one field must be provided for update");
        }

        // Validar que los campos enviados no sean solo espacios en blanco
        if (dto.getBankName() != null && dto.getBankName().trim().isEmpty()) {
            throw new MessageException("Bank name cannot be empty");
        }

        if (dto.getAccountNumber() != null && dto.getAccountNumber().trim().isEmpty()) {
            throw new MessageException("Account number cannot be empty");
        }

        if (dto.getAccountType() != null && dto.getAccountType().trim().isEmpty()) {
            throw new MessageException("Account type cannot be empty");
        }

        if (dto.getAccountHolder() != null && dto.getAccountHolder().trim().isEmpty()) {
            throw new MessageException("Account holder cannot be empty");
        }
    }

    public void validateAccountNumberForUpdate(String newAccountNumber, String currentAccountNumber, Long userId) {
        if (newAccountNumber == null || newAccountNumber.trim().isEmpty()) {
            throw new MessageException("Account number cannot be empty");
        }

        String trimmedNewAccountNumber = newAccountNumber.trim();

        // Si es el mismo número, no hay problema
        if (trimmedNewAccountNumber.equals(currentAccountNumber)) {
            return;
        }

        // Verificar que no exista otra cuenta activa con este número
        if (bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(trimmedNewAccountNumber)) {
            throw new MessageException("Account number already exists");
        }

        // Verificar que no sea propiedad de otro usuario (incluyendo eliminadas)
        Optional<BankAccount> existingAccount = bankAccountRepository.findByAccountNumberIgnoreDeleted(trimmedNewAccountNumber);
        if (existingAccount.isPresent()) {
            BankAccount existing = existingAccount.get();
            if (!existing.getUser().getId().equals(userId)) {
                throw new MessageException("Cannot use account number owned by another user");
            }
        }
    }

}
