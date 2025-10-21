package com.vaPaTi.vaPaTi.service.bankAccount;

import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.validation.AccountValidationResult;
import com.vaPaTi.vaPaTi.validation.BankAccountValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountValidationService - Account Validation Tests")
class BankAccountValidationServiceAccountValidationTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    private BankAccountValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new BankAccountValidationService(bankAccountRepository);
    }

    @Nested
    @DisplayName("validateAccountCreation method tests")
    class ValidateAccountCreationTests {

        @Test
        @DisplayName("should return CAN_CREATE when account does not exist")
        void validateAccountCreation_WithNonExistingAccount_ShouldReturnCanCreate() {
            // Given
            String accountNumber = "1234567890";
            Long userId = 1L;
            when(bankAccountRepository.findByAccountNumberIgnoreDeleted(accountNumber))
                    .thenReturn(Optional.empty());

            // When
            AccountValidationResult result = validationService.validateAccountCreation(accountNumber, userId);

            // Then
            assertEquals(AccountValidationResult.CAN_CREATE, result);
            verify(bankAccountRepository).findByAccountNumberIgnoreDeleted(accountNumber);
        }

        @Test
        @DisplayName("should return ALREADY_EXISTS when active account exists")
        void validateAccountCreation_WithActiveAccount_ShouldReturnAlreadyExists() {
            // Given
            String accountNumber = "1234567890";
            Long userId = 1L;
            BankAccount existingAccount = mock(BankAccount.class);
            User user = mock(User.class);

            when(existingAccount.getDeletedAt()).thenReturn(null);
            when(existingAccount.getUser()).thenReturn(user);
            when(user.getId()).thenReturn(userId);
            when(bankAccountRepository.findByAccountNumberIgnoreDeleted(accountNumber))
                    .thenReturn(Optional.of(existingAccount));

            // When
            AccountValidationResult result = validationService.validateAccountCreation(accountNumber, userId);

            // Then
            assertEquals(AccountValidationResult.ALREADY_EXISTS, result);
            verify(bankAccountRepository).findByAccountNumberIgnoreDeleted(accountNumber);
            verify(existingAccount).getDeletedAt();
        }

        @Test
        @DisplayName("should return OWNED_BY_OTHER_USER when deleted account is owned by different user")
        void validateAccountCreation_WithDeletedAccountOwnedByOtherUser_ShouldReturnOwnedByOtherUser() {
            // Given
            String accountNumber = "1234567890";
            Long userId = 1L;
            Long otherUserId = 2L;
            BankAccount existingAccount = mock(BankAccount.class);
            User otherUser = mock(User.class);

            when(existingAccount.getDeletedAt()).thenReturn(LocalDateTime.now());
            when(existingAccount.getUser()).thenReturn(otherUser);
            when(otherUser.getId()).thenReturn(otherUserId);
            when(bankAccountRepository.findByAccountNumberIgnoreDeleted(accountNumber))
                    .thenReturn(Optional.of(existingAccount));

            // When
            AccountValidationResult result = validationService.validateAccountCreation(accountNumber, userId);

            // Then
            assertEquals(AccountValidationResult.OWNED_BY_OTHER_USER, result);
            verify(bankAccountRepository).findByAccountNumberIgnoreDeleted(accountNumber);
            verify(existingAccount).getDeletedAt();
            verify(existingAccount).getUser();
            verify(otherUser).getId();
        }

        @Test
        @DisplayName("should return CAN_RESTORE when deleted account is owned by same user")
        void validateAccountCreation_WithDeletedAccountOwnedBySameUser_ShouldReturnCanRestore() {
            // Given
            String accountNumber = "1234567890";
            Long userId = 1L;
            BankAccount existingAccount = mock(BankAccount.class);
            User user = mock(User.class);

            when(existingAccount.getDeletedAt()).thenReturn(LocalDateTime.now());
            when(existingAccount.getUser()).thenReturn(user);
            when(user.getId()).thenReturn(userId);
            when(bankAccountRepository.findByAccountNumberIgnoreDeleted(accountNumber))
                    .thenReturn(Optional.of(existingAccount));

            // When
            AccountValidationResult result = validationService.validateAccountCreation(accountNumber, userId);

            // Then
            assertEquals(AccountValidationResult.CAN_RESTORE, result);
            verify(bankAccountRepository).findByAccountNumberIgnoreDeleted(accountNumber);
            verify(existingAccount).getDeletedAt();
            verify(existingAccount).getUser();
            verify(user).getId();
        }
    }

    @Nested
    @DisplayName("checkIfUserHasDuplicateAccount method tests")
    class CheckIfUserHasDuplicateAccountTests {

        @Test
        @DisplayName("should throw MessageException when user has duplicate account")
        void checkIfUserHasDuplicateAccount_WithDuplicateAccount_ShouldThrowException() {
            // Given
            Long userId = 1L;
            String accountNumber = "1234567890";
            when(bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, accountNumber))
                    .thenReturn(true);

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.checkIfUserHasDuplicateAccount(userId, accountNumber)
            );
            assertEquals("User already has a bank account with this account number", exception.getMessage());
            verify(bankAccountRepository).existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, accountNumber);
        }

        @Test
        @DisplayName("should not throw exception when user does not have duplicate account")
        void checkIfUserHasDuplicateAccount_WithoutDuplicateAccount_ShouldNotThrowException() {
            // Given
            Long userId = 1L;
            String accountNumber = "1234567890";
            when(bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, accountNumber))
                    .thenReturn(false);

            // When & Then
            assertDoesNotThrow(() -> validationService.checkIfUserHasDuplicateAccount(userId, accountNumber));
            verify(bankAccountRepository).existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, accountNumber);
        }
    }

    @Nested
    @DisplayName("validateAccountNumberForUpdate method tests")
    class ValidateAccountNumberForUpdateTests {

        @Test
        @DisplayName("should throw MessageException when new account number is null")
        void validateAccountNumberForUpdate_WithNullAccountNumber_ShouldThrowException() {
            // Given
            String newAccountNumber = null;
            String currentAccountNumber = "1234567890";
            Long userId = 1L;

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateAccountNumberForUpdate(newAccountNumber, currentAccountNumber, userId)
            );
            assertEquals("Account number cannot be empty", exception.getMessage());
            verifyNoInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should throw MessageException when new account number is empty")
        void validateAccountNumberForUpdate_WithEmptyAccountNumber_ShouldThrowException() {
            // Given
            String newAccountNumber = "   ";
            String currentAccountNumber = "1234567890";
            Long userId = 1L;

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateAccountNumberForUpdate(newAccountNumber, currentAccountNumber, userId)
            );
            assertEquals("Account number cannot be empty", exception.getMessage());
            verifyNoInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should not throw exception when new account number is same as current")
        void validateAccountNumberForUpdate_WithSameAccountNumber_ShouldNotThrowException() {
            // Given
            String newAccountNumber = "1234567890";
            String currentAccountNumber = "1234567890";
            Long userId = 1L;

            // When & Then
            assertDoesNotThrow(() -> validationService.validateAccountNumberForUpdate(newAccountNumber, currentAccountNumber, userId));
            verifyNoInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should throw MessageException when new account number already exists as active")
        void validateAccountNumberForUpdate_WithExistingActiveAccount_ShouldThrowException() {
            // Given
            String newAccountNumber = "9999999999";
            String currentAccountNumber = "1234567890";
            Long userId = 1L;
            when(bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(newAccountNumber))
                    .thenReturn(true);

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateAccountNumberForUpdate(newAccountNumber, currentAccountNumber, userId)
            );
            assertEquals("Account number already exists", exception.getMessage());
            verify(bankAccountRepository).existsByAccountNumberAndDeletedAtIsNull(newAccountNumber);
        }

        @Test
        @DisplayName("should throw MessageException when new account number is owned by another user")
        void validateAccountNumberForUpdate_WithDeletedAccountOwnedByOtherUser_ShouldThrowException() {
            // Given
            String newAccountNumber = "9999999999";
            String currentAccountNumber = "1234567890";
            Long userId = 1L;
            Long otherUserId = 2L;
            BankAccount existingAccount = mock(BankAccount.class);
            User otherUser = mock(User.class);

            when(bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(newAccountNumber))
                    .thenReturn(false);
            when(bankAccountRepository.findByAccountNumberIgnoreDeleted(newAccountNumber))
                    .thenReturn(Optional.of(existingAccount));
            when(existingAccount.getUser()).thenReturn(otherUser);
            when(otherUser.getId()).thenReturn(otherUserId);

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateAccountNumberForUpdate(newAccountNumber, currentAccountNumber, userId)
            );
            assertEquals("Cannot use account number owned by another user", exception.getMessage());
            verify(bankAccountRepository).existsByAccountNumberAndDeletedAtIsNull(newAccountNumber);
            verify(bankAccountRepository).findByAccountNumberIgnoreDeleted(newAccountNumber);
            verify(existingAccount).getUser();
            verify(otherUser).getId();
        }

        @Test
        @DisplayName("should not throw exception when new account number is available")
        void validateAccountNumberForUpdate_WithAvailableAccountNumber_ShouldNotThrowException() {
            // Given
            String newAccountNumber = "9999999999";
            String currentAccountNumber = "1234567890";
            Long userId = 1L;
            when(bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(newAccountNumber))
                    .thenReturn(false);
            when(bankAccountRepository.findByAccountNumberIgnoreDeleted(newAccountNumber))
                    .thenReturn(Optional.empty());

            // When & Then
            assertDoesNotThrow(() -> validationService.validateAccountNumberForUpdate(newAccountNumber, currentAccountNumber, userId));
            verify(bankAccountRepository).existsByAccountNumberAndDeletedAtIsNull(newAccountNumber);
            verify(bankAccountRepository).findByAccountNumberIgnoreDeleted(newAccountNumber);
        }
    }

}
