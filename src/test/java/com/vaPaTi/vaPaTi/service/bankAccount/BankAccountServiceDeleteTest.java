package com.vaPaTi.vaPaTi.service.bankAccount;

import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.service.BankAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountService - deleteBankAccount Tests")
class BankAccountServiceDeleteTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private BankAccountService bankAccountService;

    // Test data
    private Long validBankAccountId;
    private Long invalidBankAccountId;
    private User testUser;
    private BankAccount activeBankAccount;
    private BankAccount deletedBankAccount;
    private LocalDateTime fixedDateTime;

    @BeforeEach
    void setUp() {
        // Test data setup
        validBankAccountId = 1L;
        invalidBankAccountId = 999L;
        fixedDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        testUser = User.builder()
                .id(1L)
                .build();

        activeBankAccount = BankAccount.builder()
                .id(validBankAccountId)
                .user(testUser)
                .bankName("Banco Santander")
                .accountNumber("1234567890")
                .accountType("CHECKING")
                .accountHolder("Juan Pérez")
                .deletedAt(null) // Active account
                .build();

        deletedBankAccount = BankAccount.builder()
                .id(validBankAccountId)
                .user(testUser)
                .bankName("Banco Santander")
                .accountNumber("1234567890")
                .accountType("CHECKING")
                .accountHolder("Juan Pérez")
                .deletedAt(fixedDateTime) // Already deleted
                .build();
    }

    @Test
    @DisplayName("Should successfully delete bank account when account exists and is active")
    void deleteBankAccount_WhenAccountExistsAndIsActive_ShouldMarkAsDeleted() {
        // Given
        when(bankAccountRepository.findById(validBankAccountId))
                .thenReturn(Optional.of(activeBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenReturn(activeBankAccount);

        // When
        bankAccountService.deleteBankAccount(validBankAccountId);

        // Then
        ArgumentCaptor<BankAccount> bankAccountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(bankAccountCaptor.capture());

        BankAccount savedAccount = bankAccountCaptor.getValue();
        assertThat(savedAccount.getDeletedAt())
                .isNotNull()
                .isBeforeOrEqualTo(LocalDateTime.now())
                .isAfter(LocalDateTime.now().minusSeconds(5)); // Allow small time window

        // Verify interaction order
        InOrder inOrder = inOrder(bankAccountRepository);
        inOrder.verify(bankAccountRepository).findById(validBankAccountId);
        inOrder.verify(bankAccountRepository).save(activeBankAccount);

        verifyNoMoreInteractions(bankAccountRepository);
    }

    @Test
    @DisplayName("Should throw MessageException when bank account does not exist")
    void deleteBankAccount_WhenAccountDoesNotExist_ShouldThrowMessageException() {
        // Given
        when(bankAccountRepository.findById(invalidBankAccountId))
                .thenReturn(Optional.empty());

        // When & Then
        MessageException exception = catchThrowableOfType(
                () -> bankAccountService.deleteBankAccount(invalidBankAccountId),
                MessageException.class
        );

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank account not found with id: " + invalidBankAccountId);

        // Verify that only findById was called
        verify(bankAccountRepository).findById(invalidBankAccountId);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoMoreInteractions(bankAccountRepository);
    }

    @Test
    @DisplayName("Should throw MessageException when bank account is already deleted")
    void deleteBankAccount_WhenAccountIsAlreadyDeleted_ShouldThrowMessageException() {
        // Given
        when(bankAccountRepository.findById(validBankAccountId))
                .thenReturn(Optional.of(deletedBankAccount));

        // When & Then
        MessageException exception = catchThrowableOfType(
                () -> bankAccountService.deleteBankAccount(validBankAccountId),
                MessageException.class
        );

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank account is already deleted");

        // Verify that findById was called but save was not
        verify(bankAccountRepository).findById(validBankAccountId);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoMoreInteractions(bankAccountRepository);
    }

    @Test
    @DisplayName("Should handle null bank account id gracefully")
    void deleteBankAccount_WhenBankAccountIdIsNull_ShouldThrowMessageException() {
        // Given
        Long nullId = null;
        when(bankAccountRepository.findById(nullId))
                .thenReturn(Optional.empty());

        // When & Then
        MessageException exception = catchThrowableOfType(
                () -> bankAccountService.deleteBankAccount(nullId),
                MessageException.class
        );

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank account not found with id: null");

        verify(bankAccountRepository).findById(nullId);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Should preserve all bank account data except deletedAt when deleting")
    void deleteBankAccount_WhenDeleting_ShouldPreserveAllDataExceptDeletedAt() {
        // Given
        when(bankAccountRepository.findById(validBankAccountId))
                .thenReturn(Optional.of(activeBankAccount));

        // When
        bankAccountService.deleteBankAccount(validBankAccountId);

        // Then
        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(captor.capture());

        BankAccount savedAccount = captor.getValue();

        // Verify all original data is preserved
        assertThat(savedAccount.getId()).isEqualTo(validBankAccountId);
        assertThat(savedAccount.getUser()).isEqualTo(testUser);
        assertThat(savedAccount.getBankName()).isEqualTo("Banco Santander");
        assertThat(savedAccount.getAccountNumber()).isEqualTo("1234567890");
        assertThat(savedAccount.getAccountType()).isEqualTo("CHECKING");
        assertThat(savedAccount.getAccountHolder()).isEqualTo("Juan Pérez");

        // Only deletedAt should be modified
        assertThat(savedAccount.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should maintain correct execution order during deletion process")
    void deleteBankAccount_ShouldMaintainCorrectExecutionOrder() {
        // Given
        when(bankAccountRepository.findById(validBankAccountId))
                .thenReturn(Optional.of(activeBankAccount));

        // When
        bankAccountService.deleteBankAccount(validBankAccountId);

        // Then - Verify exact order of execution
        InOrder inOrder = inOrder(bankAccountRepository);

        // First: find the bank account
        inOrder.verify(bankAccountRepository).findById(validBankAccountId);

        // Second: save the updated bank account with deletedAt
        inOrder.verify(bankAccountRepository).save(activeBankAccount);
    }

    @Test
    @DisplayName("Should set deletedAt to current timestamp within acceptable range")
    void deleteBankAccount_WhenDeleting_ShouldSetDeletedAtToCurrentTime() {
        // Given
        LocalDateTime beforeDeletion = LocalDateTime.now();
        when(bankAccountRepository.findById(validBankAccountId))
                .thenReturn(Optional.of(activeBankAccount));

        // When
        bankAccountService.deleteBankAccount(validBankAccountId);
        LocalDateTime afterDeletion = LocalDateTime.now();

        // Then
        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(captor.capture());

        BankAccount savedAccount = captor.getValue();
        LocalDateTime deletedAt = savedAccount.getDeletedAt();

        assertThat(deletedAt)
                .isNotNull()
                .isAfterOrEqualTo(beforeDeletion)
                .isBeforeOrEqualTo(afterDeletion);
    }

    @Test
    @DisplayName("Should verify bank account state transitions correctly")
    void deleteBankAccount_WhenProcessingDeletion_ShouldTransitionStateCorrectly() {
        // Given - Start with active account
        BankAccount accountToDelete = BankAccount.builder()
                .id(validBankAccountId)
                .user(testUser)
                .bankName("Test Bank")
                .accountNumber("TEST123")
                .accountType("SAVINGS")
                .accountHolder("Test User")
                .deletedAt(null) // Initially active
                .build();

        when(bankAccountRepository.findById(validBankAccountId))
                .thenReturn(Optional.of(accountToDelete));

        // When
        bankAccountService.deleteBankAccount(validBankAccountId);

        // Then
        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(captor.capture());

        BankAccount savedAccount = captor.getValue();

        // Verify state transition from active to deleted
        assertThat(savedAccount)
                .extracting(BankAccount::getDeletedAt)
                .isNotNull();

        // Verify it's the same object reference (important for Hibernate/JPA)
        assertThat(savedAccount).isSameAs(accountToDelete);
    }

    @Test
    @DisplayName("Should handle edge case when deletedAt is set to epoch time")
    void deleteBankAccount_WhenAccountDeletedAtEpoch_ShouldStillThrowException() {
        // Given - Account with deletedAt set to epoch (edge case)
        BankAccount epochDeletedAccount = BankAccount.builder()
                .id(validBankAccountId)
                .user(testUser)
                .bankName("Banco Test")
                .accountNumber("1234567890")
                .accountType("CHECKING")
                .accountHolder("Test User")
                .deletedAt(LocalDateTime.of(1970, 1, 1, 0, 0, 0)) // Epoch time
                .build();

        when(bankAccountRepository.findById(validBankAccountId))
                .thenReturn(Optional.of(epochDeletedAccount));

        // When & Then
        MessageException exception = catchThrowableOfType(
                () -> bankAccountService.deleteBankAccount(validBankAccountId),
                MessageException.class
        );

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank account is already deleted");

        verify(bankAccountRepository).findById(validBankAccountId);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

}
