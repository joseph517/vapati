package com.vaPaTi.vaPaTi.service.bankAccount;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.BankAccountMapper;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.service.BankAccountService;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountService - getBankAccountsByUserId Tests")
class BankAccountServiceListTest {

    @Mock
    private UserValidationService userValidationService;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private BankAccountMapper bankAccountMapper;

    @InjectMocks
    private BankAccountService bankAccountService;

    // Test data
    private Long validUserId;
    private Long invalidUserId;
    private User testUser;
    private BankAccount bankAccount1;
    private BankAccount bankAccount2;
    private BankAccountDTO bankAccountDTO1;
    private BankAccountDTO bankAccountDTO2;
    private List<BankAccount> bankAccountsList;

    @BeforeEach
    void setUp() {
        // Test data setup
        validUserId = 1L;
        invalidUserId = 999L;

        testUser = User.builder()
                .id(validUserId)
                .build();

        bankAccount1 = BankAccount.builder()
                .id(1L)
                .user(testUser)
                .bankName("Banco Santander")
                .accountNumber("1234567890")
                .accountType("CHECKING")
                .accountHolder("Juan Pérez")
                .build();

        bankAccount2 = BankAccount.builder()
                .id(2L)
                .user(testUser)
                .bankName("BBVA")
                .accountNumber("0987654321")
                .accountType("SAVINGS")
                .accountHolder("Juan Pérez")
                .build();

        bankAccountDTO1 = BankAccountDTO.builder()
                .id(1L)
                .userId(validUserId)
                .bankName("Banco Santander")
                .accountNumber("1234567890")
                .accountType("CHECKING")
                .accountHolder("Juan Pérez")
                .build();

        bankAccountDTO2 = BankAccountDTO.builder()
                .id(2L)
                .userId(validUserId)
                .bankName("BBVA")
                .accountNumber("0987654321")
                .accountType("SAVINGS")
                .accountHolder("Juan Pérez")
                .build();

        bankAccountsList = Arrays.asList(bankAccount1, bankAccount2);}

    @Test
    @DisplayName("Should return list of bank accounts when user exists and has bank accounts")
    void getBankAccountsByUserId_WhenUserExistsAndHasBankAccounts_ShouldReturnBankAccountDTOList() {
        // Given
        when(userValidationService.existsById(validUserId)).thenReturn(true);
        when(bankAccountRepository.findByUserId(validUserId)).thenReturn(bankAccountsList);
        when(bankAccountMapper.toDto(bankAccount1)).thenReturn(bankAccountDTO1);
        when(bankAccountMapper.toDto(bankAccount2)).thenReturn(bankAccountDTO2);

        // When
        List<BankAccountDTO> result = bankAccountService.getBankAccountsByUserId(validUserId);

        // Then
        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .containsExactly(bankAccountDTO1, bankAccountDTO2);

        // Verify interaction order and calls
        InOrder inOrder = inOrder(userValidationService, bankAccountRepository, bankAccountMapper);
        inOrder.verify(userValidationService).existsById(validUserId);
        inOrder.verify(bankAccountRepository).findByUserId(validUserId);
        inOrder.verify(bankAccountMapper).toDto(bankAccount1);
        inOrder.verify(bankAccountMapper).toDto(bankAccount2);

        verifyNoMoreInteractions(userValidationService, bankAccountRepository, bankAccountMapper);
    }

    @Test
    @DisplayName("Should return single bank account when user exists and has one bank account")
    void getBankAccountsByUserId_WhenUserExistsAndHasOneBankAccount_ShouldReturnSingleBankAccountDTO() {
        // Given
        List<BankAccount> singleAccountList = Collections.singletonList(bankAccount1);
        when(userValidationService.existsById(validUserId)).thenReturn(true);
        when(bankAccountRepository.findByUserId(validUserId)).thenReturn(singleAccountList);
        when(bankAccountMapper.toDto(bankAccount1)).thenReturn(bankAccountDTO1);

        // When
        List<BankAccountDTO> result = bankAccountService.getBankAccountsByUserId(validUserId);

        // Then
        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsExactly(bankAccountDTO1);

        verify(userValidationService).existsById(validUserId);
        verify(bankAccountRepository).findByUserId(validUserId);
        verify(bankAccountMapper).toDto(bankAccount1);
        verifyNoMoreInteractions(userValidationService, bankAccountRepository, bankAccountMapper);
    }

    @Test
    @DisplayName("Should throw MessageException when user does not exist")
    void getBankAccountsByUserId_WhenUserDoesNotExist_ShouldThrowMessageException() {
        // Given
        when(userValidationService.existsById(invalidUserId)).thenReturn(false);

        // When & Then
        MessageException exception = catchThrowableOfType(
                () -> bankAccountService.getBankAccountsByUserId(invalidUserId),
                MessageException.class
        );

        assertThat(exception)
                .isNotNull()
                .hasMessage("User not found with id: " + invalidUserId);

        // Verify that only user validation was called
        verify(userValidationService).existsById(invalidUserId);
        verifyNoInteractions(bankAccountRepository, bankAccountMapper);
    }

    @Test
    @DisplayName("Should throw MessageException when user exists but has no bank accounts")
    void getBankAccountsByUserId_WhenUserExistsButHasNoBankAccounts_ShouldThrowMessageException() {
        // Given
        List<BankAccount> emptyList = Collections.emptyList();
        when(userValidationService.existsById(validUserId)).thenReturn(true);
        when(bankAccountRepository.findByUserId(validUserId)).thenReturn(emptyList);

        // When & Then
        MessageException exception = catchThrowableOfType(
                () -> bankAccountService.getBankAccountsByUserId(validUserId),
                MessageException.class
        );

        assertThat(exception)
                .isNotNull()
                .hasMessage("No bank accounts found for user with id: " + validUserId);

        // Verify interaction order
        InOrder inOrder = inOrder(userValidationService, bankAccountRepository);
        inOrder.verify(userValidationService).existsById(validUserId);
        inOrder.verify(bankAccountRepository).findByUserId(validUserId);

        verifyNoInteractions(bankAccountMapper);
    }

    @Test
    @DisplayName("Should handle null userId gracefully")
    void getBankAccountsByUserId_WhenUserIdIsNull_ShouldThrowMessageException() {
        // Given
        Long nullUserId = null;
        when(userValidationService.existsById(nullUserId)).thenReturn(false);

        // When & Then
        MessageException exception = catchThrowableOfType(
                () -> bankAccountService.getBankAccountsByUserId(nullUserId),
                MessageException.class
        );

        assertThat(exception)
                .isNotNull()
                .hasMessage("User not found with id: null");

        verify(userValidationService).existsById(nullUserId);
        verifyNoInteractions(bankAccountRepository, bankAccountMapper);
    }

    @Test
    @DisplayName("Should handle large list of bank accounts correctly")
    void getBankAccountsByUserId_WhenUserHasMultipleBankAccounts_ShouldReturnAllMappedAccounts() {
        // Given - Create a larger list for edge case testing
        List<BankAccount> largeBankAccountsList = Arrays.asList(
                bankAccount1, bankAccount2,
                createBankAccount(3L, "Banco Nacional"),
                createBankAccount(4L, "Banco Popular"),
                createBankAccount(5L, "BCR")
        );

        List<BankAccountDTO> largeDTOsList = Arrays.asList(
                bankAccountDTO1, bankAccountDTO2,
                createBankAccountDTO(3L, "Banco Nacional"),
                createBankAccountDTO(4L, "Banco Popular"),
                createBankAccountDTO(5L, "BCR")
        );

        when(userValidationService.existsById(validUserId)).thenReturn(true);
        when(bankAccountRepository.findByUserId(validUserId)).thenReturn(largeBankAccountsList);

        // Mock each mapping call
        for (int i = 0; i < largeBankAccountsList.size(); i++) {
            when(bankAccountMapper.toDto(largeBankAccountsList.get(i)))
                    .thenReturn(largeDTOsList.get(i));
        }

        // When
        List<BankAccountDTO> result = bankAccountService.getBankAccountsByUserId(validUserId);

        // Then
        assertThat(result)
                .isNotNull()
                .hasSize(5)
                .containsExactlyElementsOf(largeDTOsList);

        verify(userValidationService).existsById(validUserId);
        verify(bankAccountRepository).findByUserId(validUserId);

        // Verify each mapping was called
        largeBankAccountsList.forEach(account -> verify(bankAccountMapper).toDto(account));
    }

    @Test
    @DisplayName("Should maintain correct execution order when processing bank accounts")
    void getBankAccountsByUserId_ShouldMaintainCorrectExecutionOrder() {
        // Given
        when(userValidationService.existsById(validUserId)).thenReturn(true);
        when(bankAccountRepository.findByUserId(validUserId)).thenReturn(bankAccountsList);
        when(bankAccountMapper.toDto(bankAccount1)).thenReturn(bankAccountDTO1);
        when(bankAccountMapper.toDto(bankAccount2)).thenReturn(bankAccountDTO2);

        // When
        bankAccountService.getBankAccountsByUserId(validUserId);

        // Then - Verify exact order of execution
        InOrder inOrder = inOrder(userValidationService, bankAccountRepository, bankAccountMapper);

        // First: validate user exists
        inOrder.verify(userValidationService).existsById(validUserId);

        // Second: find bank accounts
        inOrder.verify(bankAccountRepository).findByUserId(validUserId);

        // Third: map each account (order matters for stream processing)
        inOrder.verify(bankAccountMapper).toDto(bankAccount1);
        inOrder.verify(bankAccountMapper).toDto(bankAccount2);
    }

    // Helper methods for test data creation
    private BankAccount createBankAccount(Long id, String bankName) {
        return BankAccount.builder()
                .id(id)
                .user(testUser)
                .bankName(bankName)
                .accountNumber("ACC" + id)
                .accountType("CHECKING")
                .accountHolder("Juan Pérez")
                .build();
    }

    private BankAccountDTO createBankAccountDTO(Long id, String bankName) {
        return BankAccountDTO.builder()
                .id(id)
                .userId(validUserId)
                .bankName(bankName)
                .accountNumber("ACC" + id)
                .accountType("CHECKING")
                .accountHolder("Juan Pérez")
                .build();
    }
}
