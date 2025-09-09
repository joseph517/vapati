package com.vaPaTi.vaPaTi.service.bankAccount;

import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateBankAccountDTO;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.validation.BankAccountValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountValidationService Tests")
class BankAccountValidationServiceInputValidationTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    private BankAccountValidationService validationService;
    private CreateBankAccountDTO validCreateDto;
    private UpdateBankAccountDTO validUpdateDto;

    @BeforeEach
    void setUp() {
        validationService = new BankAccountValidationService(bankAccountRepository);

        validCreateDto = CreateBankAccountDTO.builder()
                .userId(1L)
                .bankName("Chase Bank")
                .accountHolder("John Doe")
                .accountNumber("1234567890")
                .accountType("Checking")
                .build();

        validUpdateDto = UpdateBankAccountDTO.builder()
                .bankName("Wells Fargo")
                .accountHolder("Jane Smith")
                .accountNumber("0987654321")
                .accountType("Savings")
                .build();
    }

    @Nested
    @DisplayName("validateInput method tests")
    class ValidateInputTests {

        @Test
        @DisplayName("should pass validation when all required fields are valid")
        void shouldPassValidation_WhenAllFieldsAreValid() {
            // When & Then
            assertDoesNotThrow(() -> validationService.validateInput(validCreateDto));
        }

        @Test
        @DisplayName("should throw MessageException when bank name is null")
        void shouldThrowMessageException_WhenBankNameIsNull() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .bankName(null)
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("Bank name cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when bank name is empty string")
        void shouldThrowMessageException_WhenBankNameIsEmptyString() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .bankName("")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("Bank name cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when bank name is only whitespace")
        void shouldThrowMessageException_WhenBankNameIsOnlyWhitespace() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .bankName("   ")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("Bank name cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when account holder is null")
        void shouldThrowMessageException_WhenAccountHolderIsNull() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .accountHolder(null)
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("Account holder cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when account holder is empty")
        void shouldThrowMessageException_WhenAccountHolderIsEmpty() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .accountHolder("")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("Account holder cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when account number is null")
        void shouldThrowMessageException_WhenAccountNumberIsNull() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .accountNumber(null)
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("Account number cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when account number is empty")
        void shouldThrowMessageException_WhenAccountNumberIsEmpty() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .accountNumber("")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("Account number cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when account type is null")
        void shouldThrowMessageException_WhenAccountTypeIsNull() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .accountType(null)
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("Account type cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when account type is empty")
        void shouldThrowMessageException_WhenAccountTypeIsEmpty() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .accountType("")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("Account type cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when user ID is null")
        void shouldThrowMessageException_WhenUserIdIsNull() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .userId(null)
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateInput(dto)
            );
            assertEquals("User ID is required", exception.getMessage());
        }

        @Test
        @DisplayName("should accept valid string fields with leading and trailing spaces")
        void shouldAcceptValidFields_WithLeadingAndTrailingSpaces() {
            // Given
            CreateBankAccountDTO dto = validCreateDto.toBuilder()
                    .bankName(" Chase Bank ")
                    .accountHolder(" John Doe ")
                    .accountNumber(" 1234567890 ")
                    .accountType(" Checking ")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateInput(dto));
        }
    }

    @Nested
    @DisplayName("validateUpdateInput method tests")
    class ValidateUpdateInputTests {

        @Test
        @DisplayName("should pass validation when all fields are valid")
        void shouldPassValidation_WhenAllFieldsAreValid() {
            // When & Then
            assertDoesNotThrow(() -> validationService.validateUpdateInput(validUpdateDto));
        }

        @Test
        @DisplayName("should throw MessageException when DTO is null")
        void shouldThrowMessageException_WhenDtoIsNull() {
            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateUpdateInput(null)
            );
            assertEquals("Update data cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when all fields are null")
        void shouldThrowMessageException_WhenAllFieldsAreNull() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .bankName(null)
                    .accountNumber(null)
                    .accountType(null)
                    .accountHolder(null)
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateUpdateInput(dto)
            );
            assertEquals("At least one field must be provided for update", exception.getMessage());
        }

        @Test
        @DisplayName("should pass validation when only bank name is provided")
        void shouldPassValidation_WhenOnlyBankNameIsProvided() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .bankName("Chase Bank")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateUpdateInput(dto));
        }

        @Test
        @DisplayName("should pass validation when only account number is provided")
        void shouldPassValidation_WhenOnlyAccountNumberIsProvided() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .accountNumber("1234567890")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateUpdateInput(dto));
        }

        @Test
        @DisplayName("should pass validation when only account type is provided")
        void shouldPassValidation_WhenOnlyAccountTypeIsProvided() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .accountType("Checking")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateUpdateInput(dto));
        }

        @Test
        @DisplayName("should pass validation when only account holder is provided")
        void shouldPassValidation_WhenOnlyAccountHolderIsProvided() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .accountHolder("John Doe")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateUpdateInput(dto));
        }

        @Test
        @DisplayName("should throw MessageException when bank name is empty string")
        void shouldThrowMessageException_WhenBankNameIsEmptyString() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .bankName("")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateUpdateInput(dto)
            );
            assertEquals("Bank name cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when bank name is only whitespace")
        void shouldThrowMessageException_WhenBankNameIsOnlyWhitespace() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .bankName("   ")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateUpdateInput(dto)
            );
            assertEquals("Bank name cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when account number is empty string")
        void shouldThrowMessageException_WhenAccountNumberIsEmptyString() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .accountNumber("")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateUpdateInput(dto)
            );
            assertEquals("Account number cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when account type is empty string")
        void shouldThrowMessageException_WhenAccountTypeIsEmptyString() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .accountType("")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateUpdateInput(dto)
            );
            assertEquals("Account type cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should throw MessageException when account holder is empty string")
        void shouldThrowMessageException_WhenAccountHolderIsEmptyString() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .accountHolder("")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateUpdateInput(dto)
            );
            assertEquals("Account holder cannot be empty", exception.getMessage());
        }

        @Test
        @DisplayName("should pass validation with mixed null and valid fields")
        void shouldPassValidation_WithMixedNullAndValidFields() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .bankName("Chase Bank")
                    .accountNumber(null)
                    .accountType("Checking")
                    .accountHolder(null)
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateUpdateInput(dto));
        }

        @Test
        @DisplayName("should accept valid fields with leading and trailing spaces")
        void shouldAcceptValidFields_WithLeadingAndTrailingSpaces() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .bankName(" Wells Fargo ")
                    .accountNumber(" 0987654321 ")
                    .accountType(" Savings ")
                    .accountHolder(" Jane Smith ")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateUpdateInput(dto));
        }

        @Test
        @DisplayName("should throw exception for first invalid field when multiple fields are invalid")
        void shouldThrowExceptionForFirstInvalidField_WhenMultipleFieldsAreInvalid() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .bankName("")  // First field to be validated that's invalid
                    .accountNumber("")
                    .build();

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.validateUpdateInput(dto)
            );
            assertEquals("Bank name cannot be empty", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Edge cases and integration tests")
    class EdgeCasesAndIntegrationTests {

        @Test
        @DisplayName("should validate create DTO with minimum valid values")
        void shouldValidateCreateDto_WithMinimumValidValues() {
            // Given
            CreateBankAccountDTO dto = CreateBankAccountDTO.builder()
                    .userId(1L)
                    .bankName("A")
                    .accountHolder("B")
                    .accountNumber("1")
                    .accountType("C")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateInput(dto));
        }

        @Test
        @DisplayName("should validate update DTO with single character fields")
        void shouldValidateUpdateDto_WithSingleCharacterFields() {
            // Given
            UpdateBankAccountDTO dto = UpdateBankAccountDTO.builder()
                    .bankName("A")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateUpdateInput(dto));
        }

        @Test
        @DisplayName("should handle special characters in valid fields")
        void shouldHandleSpecialCharacters_InValidFields() {
            // Given
            CreateBankAccountDTO createDto = validCreateDto.toBuilder()
                    .bankName("Chase & Co.")
                    .accountHolder("O'Connor, John Jr.")
                    .accountNumber("123-456-789")
                    .accountType("Checking/Savings")
                    .build();

            UpdateBankAccountDTO updateDto = UpdateBankAccountDTO.builder()
                    .bankName("Wells Fargo & Co.")
                    .accountHolder("Smith-Jones, Jane")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validationService.validateInput(createDto));
            assertDoesNotThrow(() -> validationService.validateUpdateInput(updateDto));
        }
    }

}
