package com.vaPaTi.vaPaTi.service.bankAccount;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.BankAccountMapper;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.service.BankAccountService;
import com.vaPaTi.vaPaTi.validation.AccountValidationResult;
import com.vaPaTi.vaPaTi.validation.BankAccountValidationService;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
    import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountService - Create Bank Account Tests")
class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private BankAccountValidationService bankAccountValidationService;
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private BankAccountMapper bankAccountMapper;
    @InjectMocks
    private BankAccountService bankAccountService;

    private CreateBankAccountDTO createDto;
    private User mockUser;
    private BankAccount mockBankAccount;
    private BankAccount savedBankAccount;
    private BankAccountDTO expectedDto;

    @BeforeEach
    void setUp() {
        // Setup CreateBankAccountDTO
        createDto = CreateBankAccountDTO.builder()
                .userId(1L)
                .bankName("Banco Test")
                .accountNumber("1234567890")
                .accountType("Corriente")
                .accountHolder("John Doe")
                .build();

        // Setup User
        mockUser = User.builder()
                .id(1L)
                .active(true)
                .verified(true)
                .build();
        mockUser.setId(1L);
        mockUser.setActive(true);
        mockUser.setVerified(true);

        // Setup BankAccount entity
        mockBankAccount = BankAccount.builder()
                .id(null)
                .user(mockUser)
                .bankName("Banco Test")
                .accountNumber("1234567890")
                .accountType("Corriente")
                .accountHolder("John Doe")
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup saved BankAccount (with ID)
        savedBankAccount = BankAccount.builder()
                .id(1L)
                .user(mockUser)
                .bankName("Banco Test")
                .accountNumber("1234567890")
                .accountType("Corriente")
                .accountHolder("John Doe")
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup expected DTO
        expectedDto = BankAccountDTO.builder()
                .id(1L)
                .userId(1L)
                .bankName("Banco Test")
                .accountNumber("1234567890")
                .accountType("Corriente")
                .accountHolder("John Doe")
                .build();
    }

    @Test
    void testBankAccountDTO() {
        BankAccountDTO dto1 = new BankAccountDTO();
        BankAccountDTO dto2 = new BankAccountDTO(1L, 2L, "Banco", "123", "Ahorro", "Juan");
        BankAccountDTO dto3 = BankAccountDTO.builder()
                .id(1L)
                .userId(2L)
                .bankName("Bank")
                .build();

        assertEquals(1L, dto3.getId());
        assertEquals("Bank", dto3.getBankName());

        dto1.setBankName("New Bank");
        assertEquals("New Bank", dto1.getBankName());

        assertNotEquals(dto1, dto2);
        assertNotNull(dto1.toString());
        assertEquals(dto1.hashCode(), dto1.hashCode());
    }

    @Test
    @DisplayName("Should create new bank account successfully when validation result is CAN_CREATE")
    void createBankAccount_ShouldCreateNewAccount_WhenValidationResultIsCanCreate() {
        // Arrange
        when(userValidationService.getUserById(1L)).thenReturn(mockUser);
        when(bankAccountValidationService.validateAccountCreation("1234567890", 1L))
                .thenReturn(AccountValidationResult.CAN_CREATE);
        when(bankAccountValidationService.buildBankAccountEntity(createDto, mockUser))
                .thenReturn(mockBankAccount);
        when(bankAccountRepository.save(mockBankAccount)).thenReturn(savedBankAccount);
        when(bankAccountMapper.toDto(savedBankAccount)).thenReturn(expectedDto);

        // Act
        BankAccountDTO result = bankAccountService.createBankAccount(createDto);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDto.getId(), result.getId());
        assertEquals(expectedDto.getUserId(), result.getUserId());
        assertEquals(expectedDto.getBankName(), result.getBankName());
        assertEquals(expectedDto.getAccountNumber(), result.getAccountNumber());
        assertEquals(expectedDto.getAccountType(), result.getAccountType());
        assertEquals(expectedDto.getAccountHolder(), result.getAccountHolder());

        // Verify all validations were called
        verify(bankAccountValidationService).validateInput(createDto);
        verify(userValidationService).getUserById(1L);
        verify(bankAccountValidationService).verifyUserIsVerified(1L);
        verify(bankAccountValidationService).checkIfUserHasDuplicateAccount(1L, "1234567890");
        verify(bankAccountValidationService).validateAccountCreation("1234567890", 1L);
        verify(bankAccountValidationService).buildBankAccountEntity(createDto, mockUser);
        verify(bankAccountRepository).save(mockBankAccount);
        verify(bankAccountMapper).toDto(savedBankAccount);
    }

    @Test
    @DisplayName("Should restore deleted bank account when validation result is CAN_RESTORE")
    void createBankAccount_ShouldRestoreDeletedAccount_WhenValidationResultIsCanRestore() {
        // Arrange
        BankAccount deletedAccount = BankAccount.builder()
                .id(2L)
                .user(mockUser)
                .bankName("Banco Test")
                .accountNumber("1234567890")
                .accountType("Corriente")
                .accountHolder("John Doe")
                .deletedAt(LocalDateTime.now().minusDays(1))
                .build();

        BankAccount restoredAccount = BankAccount.builder()
                .id(2L)
                .user(mockUser)
                .bankName("Banco Test")
                .accountNumber("1234567890")
                .accountType("Corriente")
                .accountHolder("John Doe")
                .deletedAt(null)
                .build();

        BankAccountDTO restoredDto = BankAccountDTO.builder()
                .id(2L)
                .userId(1L)
                .bankName("Banco Test")
                .accountNumber("1234567890")
                .accountType("Corriente")
                .accountHolder("John Doe")
                .build();

        when(userValidationService.getUserById(1L)).thenReturn(mockUser);
        when(bankAccountValidationService.validateAccountCreation("1234567890", 1L))
                .thenReturn(AccountValidationResult.CAN_RESTORE);
        when(bankAccountRepository.findByUserIdAndAccountNumberAndDeletedAtIsNotNull(1L, "1234567890"))
                .thenReturn(Optional.of(deletedAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(restoredAccount);
        when(bankAccountMapper.toDto(restoredAccount)).thenReturn(restoredDto);

        // Act
        BankAccountDTO result = bankAccountService.createBankAccount(createDto);

        // Assert
        assertNotNull(result);
        assertEquals(restoredDto.getId(), result.getId());
        assertEquals(restoredDto.getAccountNumber(), result.getAccountNumber());

        // Verify restoration process
        verify(bankAccountValidationService).validateInput(createDto);
        verify(userValidationService).getUserById(1L);
        verify(bankAccountValidationService).verifyUserIsVerified(1L);
        verify(bankAccountValidationService).checkIfUserHasDuplicateAccount(1L, "1234567890");
        verify(bankAccountValidationService).validateAccountCreation("1234567890", 1L);
        verify(bankAccountRepository).findByUserIdAndAccountNumberAndDeletedAtIsNotNull(1L, "1234567890");
        verify(bankAccountRepository).save(argThat(account -> account.getDeletedAt() == null));
        verify(bankAccountMapper).toDto(restoredAccount);

        // Verify that buildBankAccountEntity was NOT called (since we're restoring)
        verify(bankAccountValidationService, never()).buildBankAccountEntity(any(), any());
    }

    @Test
    @DisplayName("Should throw MessageException when trying to restore but account not found")
    void createBankAccount_ShouldThrowException_WhenCanRestoreButAccountNotFound() {
        // Arrange
        when(userValidationService.getUserById(1L)).thenReturn(mockUser);
        when(bankAccountValidationService.validateAccountCreation("1234567890", 1L))
                .thenReturn(AccountValidationResult.CAN_RESTORE);
        when(bankAccountRepository.findByUserIdAndAccountNumberAndDeletedAtIsNotNull(1L, "1234567890"))
                .thenReturn(Optional.empty());

        // Act & Assert
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.createBankAccount(createDto));

        assertEquals("Account not found for restoration", exception.getMessage());

        // Verify that save and mapper were not called
        verify(bankAccountRepository, never()).save(any());
        verify(bankAccountMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should throw MessageException when validation result is ALREADY_EXISTS")
    void createBankAccount_ShouldThrowException_WhenValidationResultIsAlreadyExists() {
        // Arrange
        when(userValidationService.getUserById(1L)).thenReturn(mockUser);
        when(bankAccountValidationService.validateAccountCreation("1234567890", 1L))
                .thenReturn(AccountValidationResult.ALREADY_EXISTS);

        // Act & Assert
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.createBankAccount(createDto));

        assertEquals("Account number already exists", exception.getMessage());

        // Verify that repository operations were not called
        verify(bankAccountRepository, never()).save(any());
        verify(bankAccountRepository, never()).findByUserIdAndAccountNumberAndDeletedAtIsNotNull(anyLong(), anyString());
        verify(bankAccountMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should throw MessageException when validation result is OWNED_BY_OTHER_USER")
    void createBankAccount_ShouldThrowException_WhenValidationResultIsOwnedByOtherUser() {
        // Arrange
        when(userValidationService.getUserById(1L)).thenReturn(mockUser);
        when(bankAccountValidationService.validateAccountCreation("1234567890", 1L))
                .thenReturn(AccountValidationResult.OWNED_BY_OTHER_USER);

        // Act & Assert
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.createBankAccount(createDto));

        assertEquals("Cannot create account with this number", exception.getMessage());

        // Verify that repository operations were not called
        verify(bankAccountRepository, never()).save(any());
        verify(bankAccountRepository, never()).findByUserIdAndAccountNumberAndDeletedAtIsNotNull(anyLong(), anyString());
        verify(bankAccountMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should throw NullPointerException when validation result is null")
    void createBankAccount_ShouldThrowNullPointerException_WhenValidationResultIsNull() {
        // Arrange
        when(userValidationService.getUserById(1L)).thenReturn(mockUser);
        when(bankAccountValidationService.validateAccountCreation("1234567890", 1L))
                .thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> bankAccountService.createBankAccount(createDto));
    }

    @Test
    @DisplayName("Should propagate MessageException from validation services")
    void createBankAccount_ShouldPropagateException_WhenValidationServiceThrows() {
        // Arrange
        String expectedMessage = "User validation failed";
        when(userValidationService.getUserById(1L))
                .thenThrow(new MessageException(expectedMessage));

        // Act & Assert
        MessageException exception = assertThrows(MessageException.class,
                () -> bankAccountService.createBankAccount(createDto));

        assertEquals(expectedMessage, exception.getMessage());

        // Verify that subsequent operations were not called
        verify(bankAccountValidationService).validateInput(createDto);
        verify(bankAccountValidationService, never()).verifyUserIsVerified(anyLong());
        verify(bankAccountValidationService, never()).checkIfUserHasDuplicateAccount(anyLong(), anyString());
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should call all validation steps in correct order")
    void createBankAccount_ShouldCallValidationsInCorrectOrder() {
        // Arrange
        when(userValidationService.getUserById(1L)).thenReturn(mockUser);
        when(bankAccountValidationService.validateAccountCreation("1234567890", 1L))
                .thenReturn(AccountValidationResult.CAN_CREATE);
        when(bankAccountValidationService.buildBankAccountEntity(createDto, mockUser))
                .thenReturn(mockBankAccount);
        when(bankAccountRepository.save(mockBankAccount)).thenReturn(savedBankAccount);
        when(bankAccountMapper.toDto(savedBankAccount)).thenReturn(expectedDto);

        // Act
        bankAccountService.createBankAccount(createDto);

        // Assert - Verify order using InOrder
        var inOrder = inOrder(bankAccountValidationService, userValidationService,
                bankAccountRepository, bankAccountMapper);

        inOrder.verify(bankAccountValidationService).validateInput(createDto);
        inOrder.verify(userValidationService).getUserById(1L);
        inOrder.verify(bankAccountValidationService).verifyUserIsVerified(1L);
        inOrder.verify(bankAccountValidationService).checkIfUserHasDuplicateAccount(1L, "1234567890");
        inOrder.verify(bankAccountValidationService).validateAccountCreation("1234567890", 1L);
        inOrder.verify(bankAccountValidationService).buildBankAccountEntity(createDto, mockUser);
        inOrder.verify(bankAccountRepository).save(mockBankAccount);
        inOrder.verify(bankAccountMapper).toDto(savedBankAccount);
    }

}