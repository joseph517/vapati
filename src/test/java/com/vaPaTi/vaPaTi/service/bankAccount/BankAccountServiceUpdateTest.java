package com.vaPaTi.vaPaTi.service.bankAccount;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateBankAccountDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.BankAccountMapper;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.service.BankAccountService;
import com.vaPaTi.vaPaTi.validation.BankAccountValidationService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountService - Update Bank Account Tests")
class BankAccountServiceUpdateTest {

    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private BankAccountValidationService bankAccountValidationService;
    @Mock
    private BankAccountMapper bankAccountMapper;
    @InjectMocks
    private BankAccountService bankAccountService;

    private Long accountId;
    private UpdateBankAccountDTO updateDto;
    private User mockUser;
    private BankAccount existingBankAccount;
    private BankAccount updatedBankAccount;
    private BankAccountDTO expectedDto;

    @BeforeEach
    void setUp() {
        accountId = 1L;

        // Setup User
        mockUser = User.builder()
                .id(1L)
                .build();

        // Setup existing BankAccount
        existingBankAccount = BankAccount.builder()
                .id(accountId)
                .user(mockUser)
                .bankName("Old Bank")
                .accountNumber("0000000000")
                .accountType("Savings")
                .accountHolder("Old Holder")
                .isVerified(false)
                .deletedAt(null)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        // Setup UpdateBankAccountDTO with all fields
        updateDto = new UpdateBankAccountDTO();
        updateDto.setBankName("New Bank");
        updateDto.setAccountNumber("1111111111");
        updateDto.setAccountType("Checking");
        updateDto.setAccountHolder("New Holder");

        // Setup updated BankAccount
        updatedBankAccount = BankAccount.builder()
                .id(accountId)
                .user(mockUser)
                .bankName("New Bank")
                .accountNumber("1111111111")
                .accountType("Checking")
                .accountHolder("New Holder")
                .isVerified(false)
                .deletedAt(null)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup expected DTO
        expectedDto = BankAccountDTO.builder()
                .id(accountId)
                .userId(1L)
                .bankName("New Bank")
                .accountNumber("1111111111")
                .accountType("Checking")
                .accountHolder("New Holder")
                .build();
    }

    @Test
    @DisplayName("Should successfully update bank account with all fields")
    void shouldSuccessfullyUpdateBankAccountWithAllFields() {
        // Given
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(updatedBankAccount);
        when(bankAccountMapper.toDto(updatedBankAccount)).thenReturn(expectedDto);

        // When
        BankAccountDTO result = bankAccountService.updateBankAccount(accountId, updateDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedDto);

        InOrder inOrder = inOrder(bankAccountValidationService, bankAccountRepository, bankAccountMapper);
        inOrder.verify(bankAccountValidationService).validateUpdateInput(updateDto);
        inOrder.verify(bankAccountRepository).findById(accountId);
        inOrder.verify(bankAccountValidationService).validateAccountNumberForUpdate(
                "1111111111", "0000000000", 1L);
        inOrder.verify(bankAccountRepository).save(any(BankAccount.class));
        inOrder.verify(bankAccountMapper).toDto(updatedBankAccount);

        // Verify the saved bank account has updated values
        ArgumentCaptor<BankAccount> bankAccountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(bankAccountCaptor.capture());
        BankAccount savedAccount = bankAccountCaptor.getValue();
        assertThat(savedAccount.getBankName()).isEqualTo("New Bank");
        assertThat(savedAccount.getAccountNumber()).isEqualTo("1111111111");
        assertThat(savedAccount.getAccountType()).isEqualTo("Checking");
        assertThat(savedAccount.getAccountHolder()).isEqualTo("New Holder");
    }

    @Test
    @DisplayName("Should update only bank name when only bank name is provided")
    void shouldUpdateOnlyBankNameWhenOnlyBankNameProvided() {
        // Given
        UpdateBankAccountDTO partialDto = new UpdateBankAccountDTO();
        partialDto.setBankName("Updated Bank Only");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(existingBankAccount);
        when(bankAccountMapper.toDto(any(BankAccount.class))).thenReturn(expectedDto);

        // When
        bankAccountService.updateBankAccount(accountId, partialDto);

        // Then
        ArgumentCaptor<BankAccount> bankAccountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(bankAccountCaptor.capture());
        BankAccount savedAccount = bankAccountCaptor.getValue();

        assertThat(savedAccount.getBankName()).isEqualTo("Updated Bank Only");
        assertThat(savedAccount.getAccountNumber()).isEqualTo("0000000000"); // Unchanged
        assertThat(savedAccount.getAccountType()).isEqualTo("Savings"); // Unchanged
        assertThat(savedAccount.getAccountHolder()).isEqualTo("Old Holder"); // Unchanged

        verify(bankAccountValidationService, never()).validateAccountNumberForUpdate(any(), any(), any());
    }

    @Test
    @DisplayName("Should update only account number when only account number is provided")
    void shouldUpdateOnlyAccountNumberWhenOnlyAccountNumberProvided() {
        // Given
        UpdateBankAccountDTO partialDto = new UpdateBankAccountDTO();
        partialDto.setAccountNumber("9999999999");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(existingBankAccount);
        when(bankAccountMapper.toDto(any(BankAccount.class))).thenReturn(expectedDto);

        // When
        bankAccountService.updateBankAccount(accountId, partialDto);

        // Then
        verify(bankAccountValidationService).validateAccountNumberForUpdate(
                "9999999999", "0000000000", 1L);

        ArgumentCaptor<BankAccount> bankAccountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(bankAccountCaptor.capture());
        BankAccount savedAccount = bankAccountCaptor.getValue();

        assertThat(savedAccount.getAccountNumber()).isEqualTo("9999999999");
        assertThat(savedAccount.getBankName()).isEqualTo("Old Bank"); // Unchanged
    }

    @Test
    @DisplayName("Should trim whitespace from all fields when updating")
    void shouldTrimWhitespaceFromAllFieldsWhenUpdating() {
        // Given
        UpdateBankAccountDTO dtoWithWhitespace = new UpdateBankAccountDTO();
        dtoWithWhitespace.setBankName("  Trimmed Bank  ");
        dtoWithWhitespace.setAccountNumber("  1234567890  ");
        dtoWithWhitespace.setAccountType("  Checking  ");
        dtoWithWhitespace.setAccountHolder("  Trimmed Holder  ");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(existingBankAccount);
        when(bankAccountMapper.toDto(any(BankAccount.class))).thenReturn(expectedDto);

        // When
        bankAccountService.updateBankAccount(accountId, dtoWithWhitespace);

        // Then
        ArgumentCaptor<BankAccount> bankAccountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(bankAccountCaptor.capture());
        BankAccount savedAccount = bankAccountCaptor.getValue();

        assertThat(savedAccount.getBankName()).isEqualTo("Trimmed Bank");
        assertThat(savedAccount.getAccountNumber()).isEqualTo("1234567890");
        assertThat(savedAccount.getAccountType()).isEqualTo("Checking");
        assertThat(savedAccount.getAccountHolder()).isEqualTo("Trimmed Holder");
    }

    @Test
    @DisplayName("Should throw MessageException when validation service throws exception")
    void shouldThrowMessageExceptionWhenValidationServiceThrowsException() {
        // Given
        String validationErrorMessage = "Update data cannot be null";
        doThrow(new MessageException(validationErrorMessage))
                .when(bankAccountValidationService).validateUpdateInput(updateDto);

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, updateDto));

        assertThat(exception.getMessage()).isEqualTo(validationErrorMessage);
        verify(bankAccountRepository, never()).findById(any());
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw MessageException when bank account not found")
    void shouldThrowMessageExceptionWhenBankAccountNotFound() {
        // Given
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, updateDto));

        assertThat(exception.getMessage()).isEqualTo("Bank account not found with id: " + accountId);
        verify(bankAccountValidationService).validateUpdateInput(updateDto);
        verify(bankAccountRepository).findById(accountId);
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw MessageException when bank account is deleted")
    void shouldThrowMessageExceptionWhenBankAccountIsDeleted() {
        // Given
        BankAccount deletedAccount = BankAccount.builder()
                .id(accountId)
                .user(mockUser)
                .bankName("Deleted Bank")
                .accountNumber("0000000000")
                .accountType("Savings")
                .accountHolder("Deleted Holder")
                .deletedAt(LocalDateTime.now())
                .build();

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(deletedAccount));

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, updateDto));

        assertThat(exception.getMessage()).isEqualTo("Cannot update deleted bank account");
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw MessageException when bank name is empty string")
    void shouldThrowMessageExceptionWhenBankNameIsEmpty() {
        // Given
        UpdateBankAccountDTO dtoWithEmptyBankName = new UpdateBankAccountDTO();
        dtoWithEmptyBankName.setBankName("");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, dtoWithEmptyBankName));

        assertThat(exception.getMessage()).isEqualTo("Bank name cannot be empty");
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw MessageException when bank name is only whitespace")
    void shouldThrowMessageExceptionWhenBankNameIsOnlyWhitespace() {
        // Given
        UpdateBankAccountDTO dtoWithWhitespaceBankName = new UpdateBankAccountDTO();
        dtoWithWhitespaceBankName.setBankName("   ");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, dtoWithWhitespaceBankName));

        assertThat(exception.getMessage()).isEqualTo("Bank name cannot be empty");
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw MessageException when account type is empty string")
    void shouldThrowMessageExceptionWhenAccountTypeIsEmpty() {
        // Given
        UpdateBankAccountDTO dtoWithEmptyAccountType = new UpdateBankAccountDTO();
        dtoWithEmptyAccountType.setAccountType("");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, dtoWithEmptyAccountType));

        assertThat(exception.getMessage()).isEqualTo("Account type cannot be empty");
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw MessageException when account type is only whitespace")
    void shouldThrowMessageExceptionWhenAccountTypeIsOnlyWhitespace() {
        // Given
        UpdateBankAccountDTO dtoWithWhitespaceAccountType = new UpdateBankAccountDTO();
        dtoWithWhitespaceAccountType.setAccountType("   ");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, dtoWithWhitespaceAccountType));

        assertThat(exception.getMessage()).isEqualTo("Account type cannot be empty");
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw MessageException when account holder is empty string")
    void shouldThrowMessageExceptionWhenAccountHolderIsEmpty() {
        // Given
        UpdateBankAccountDTO dtoWithEmptyAccountHolder = new UpdateBankAccountDTO();
        dtoWithEmptyAccountHolder.setAccountHolder("");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, dtoWithEmptyAccountHolder));

        assertThat(exception.getMessage()).isEqualTo("Account holder cannot be empty");
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw MessageException when account holder is only whitespace")
    void shouldThrowMessageExceptionWhenAccountHolderIsOnlyWhitespace() {
        // Given
        UpdateBankAccountDTO dtoWithWhitespaceAccountHolder = new UpdateBankAccountDTO();
        dtoWithWhitespaceAccountHolder.setAccountHolder("   ");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, dtoWithWhitespaceAccountHolder));

        assertThat(exception.getMessage()).isEqualTo("Account holder cannot be empty");
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw MessageException when account number validation fails")
    void shouldThrowMessageExceptionWhenAccountNumberValidationFails() {
        // Given
        UpdateBankAccountDTO dtoWithInvalidAccountNumber = new UpdateBankAccountDTO();
        dtoWithInvalidAccountNumber.setAccountNumber("invalid-account");

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));
        doThrow(new MessageException("Account number already exists"))
                .when(bankAccountValidationService).validateAccountNumberForUpdate(
                        "invalid-account", "0000000000", 1L);

        // When & Then
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.updateBankAccount(accountId, dtoWithInvalidAccountNumber));

        assertThat(exception.getMessage()).isEqualTo("Account number already exists");
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle null values correctly and not update null fields")
    void shouldHandleNullValuesCorrectlyAndNotUpdateNullFields() {
        // Given
        UpdateBankAccountDTO dtoWithNulls = new UpdateBankAccountDTO();
        dtoWithNulls.setBankName(null);
        dtoWithNulls.setAccountNumber(null);
        dtoWithNulls.setAccountType(null);
        dtoWithNulls.setAccountHolder(null);

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(existingBankAccount);
        when(bankAccountMapper.toDto(any(BankAccount.class))).thenReturn(expectedDto);

        // When
        bankAccountService.updateBankAccount(accountId, dtoWithNulls);

        // Then
        ArgumentCaptor<BankAccount> bankAccountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(bankAccountCaptor.capture());
        BankAccount savedAccount = bankAccountCaptor.getValue();

        // Verify original values remain unchanged
        assertThat(savedAccount.getBankName()).isEqualTo("Old Bank");
        assertThat(savedAccount.getAccountNumber()).isEqualTo("0000000000");
        assertThat(savedAccount.getAccountType()).isEqualTo("Savings");
        assertThat(savedAccount.getAccountHolder()).isEqualTo("Old Holder");

        verify(bankAccountValidationService, never()).validateAccountNumberForUpdate(any(), any(), any());
    }

    @Test
    @DisplayName("Should execute operations in correct order for successful update")
    void shouldExecuteOperationsInCorrectOrderForSuccessfulUpdate() W{
        // Given
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(updatedBankAccount);
        when(bankAccountMapper.toDto(updatedBankAccount)).thenReturn(expectedDto);

        // When
        bankAccountService.updateBankAccount(accountId, updateDto);

        // Then - Verify exact order of operations
        InOrder inOrder = inOrder(bankAccountValidationService, bankAccountRepository, bankAccountMapper);

        inOrder.verify(bankAccountValidationService).validateUpdateInput(updateDto);
        inOrder.verify(bankAccountRepository).findById(accountId);
        inOrder.verify(bankAccountValidationService).validateAccountNumberForUpdate(
                "1111111111",
                "0000000000",
                1L);
        inOrder.verify(bankAccountRepository).save(any(BankAccount.class));
        inOrder.verify(bankAccountMapper).toDto(updatedBankAccount);
    }

    @Test
    @DisplayName("Should update multiple fields with mixed null and valid values")
    void shouldUpdateMultipleFieldsWithMixedNullAndValidValues() {
        // Given
        UpdateBankAccountDTO mixedDto = new UpdateBankAccountDTO();
        mixedDto.setBankName("Updated Bank");
        mixedDto.setAccountNumber(null); // Should not update
        mixedDto.setAccountType("Updated Type");
        mixedDto.setAccountHolder(null); // Should not update

        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(existingBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(existingBankAccount);
        when(bankAccountMapper.toDto(any(BankAccount.class))).thenReturn(expectedDto);

        // When
        bankAccountService.updateBankAccount(accountId, mixedDto);

        // Then
        ArgumentCaptor<BankAccount> bankAccountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(bankAccountCaptor.capture());
        BankAccount savedAccount = bankAccountCaptor.getValue();

        assertThat(savedAccount.getBankName()).isEqualTo("Updated Bank");
        assertThat(savedAccount.getAccountNumber()).isEqualTo("0000000000"); // Unchanged
        assertThat(savedAccount.getAccountType()).isEqualTo("Updated Type");
        assertThat(savedAccount.getAccountHolder()).isEqualTo("Old Holder"); // Unchanged

        verify(bankAccountValidationService, never()).validateAccountNumberForUpdate(any(), any(), any());
    }

}
