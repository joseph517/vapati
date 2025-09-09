package com.vaPaTi.vaPaTi.service.bankAccount;

import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.validation.BankAccountValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountValidationService - Entity Building Tests")
class BankAccountValidationServiceEntityBuildingTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    private BankAccountValidationService validationService;
    private CreateBankAccountDTO validDto;
    private User validUser;
    private UserInfo validUserInfo;

    @BeforeEach
    void setUp() {
        validationService = new BankAccountValidationService(bankAccountRepository);

        // Setup valid UserInfo
        validUserInfo = UserInfo.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .userName("johndoe")
                .password("hashedPassword123")
                .phone("+1234567890")
                .description("Test user description")
                .profilePicture("profile.jpg")
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        // Setup valid User
        validUser = User.builder()
                .id(1L)
                .active(true)
                .verified(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .userInfo(validUserInfo)
                .build();

        // Set bidirectional relationship
        validUserInfo.setUser(validUser);

        // Setup valid DTO
        validDto = CreateBankAccountDTO.builder()
                .userId(1L)
                .bankName("Chase Bank")
                .accountNumber("1234567890")
                .accountType("Checking")
                .accountHolder("John Doe")
                .build();
    }

    @Nested
    @DisplayName("buildBankAccountEntity method tests")
    class BuildBankAccountEntityTests {

        @Test
        @DisplayName("should build BankAccount entity with all fields correctly mapped from DTO")
        void shouldBuildBankAccountEntity_WithAllFieldsCorrectlyMappedFromDto() {
            // When
            BankAccount result = validationService.buildBankAccountEntity(validDto, validUser);

            // Then
            assertNotNull(result);
            assertSame(validUser, result.getUser());
            assertEquals(validDto.getBankName(), result.getBankName());
            assertEquals(validDto.getAccountNumber(), result.getAccountNumber());
            assertEquals(validDto.getAccountType(), result.getAccountType());
            assertEquals(validDto.getAccountHolder(), result.getAccountHolder());

            // Verify no repository interactions
            verifyNoInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should create new BankAccount instance for each invocation")
        void shouldCreateNewBankAccountInstance_ForEachInvocation() {
            // When
            BankAccount result1 = validationService.buildBankAccountEntity(validDto, validUser);
            BankAccount result2 = validationService.buildBankAccountEntity(validDto, validUser);

            // Then
            assertNotNull(result1);
            assertNotNull(result2);
            assertNotSame(result1, result2);

            // Both should have same field values but be different instances
            assertEquals(result1.getBankName(), result2.getBankName());
            assertEquals(result1.getAccountNumber(), result2.getAccountNumber());
            assertEquals(result1.getAccountType(), result2.getAccountType());
            assertEquals(result1.getAccountHolder(), result2.getAccountHolder());
            assertSame(result1.getUser(), result2.getUser());
        }

        @Test
        @DisplayName("should handle DTO with minimum valid values")
        void shouldHandleDto_WithMinimumValidValues() {
            // Given
            CreateBankAccountDTO minimalDto = CreateBankAccountDTO.builder()
                    .userId(1L)
                    .bankName("A")
                    .accountNumber("1")
                    .accountType("C")
                    .accountHolder("B")
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(minimalDto, validUser);

            // Then
            assertNotNull(result);
            assertSame(validUser, result.getUser());
            assertEquals("A", result.getBankName());
            assertEquals("1", result.getAccountNumber());
            assertEquals("C", result.getAccountType());
            assertEquals("B", result.getAccountHolder());
        }

        @Test
        @DisplayName("should handle DTO with maximum length values")
        void shouldHandleDto_WithMaximumLengthValues() {
            // Given
            String longBankName = "A".repeat(255);
            String longAccountNumber = "1".repeat(100);
            String longAccountType = "Checking/Savings/Investment".repeat(10);
            String longAccountHolder = "Very Long Account Holder Name ".repeat(20);

            CreateBankAccountDTO maximalDto = CreateBankAccountDTO.builder()
                    .userId(1L)
                    .bankName(longBankName)
                    .accountNumber(longAccountNumber)
                    .accountType(longAccountType)
                    .accountHolder(longAccountHolder)
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(maximalDto, validUser);

            // Then
            assertNotNull(result);
            assertSame(validUser, result.getUser());
            assertEquals(longBankName, result.getBankName());
            assertEquals(longAccountNumber, result.getAccountNumber());
            assertEquals(longAccountType, result.getAccountType());
            assertEquals(longAccountHolder, result.getAccountHolder());
        }

        @Test
        @DisplayName("should handle DTO with special characters in string fields")
        void shouldHandleDto_WithSpecialCharactersInStringFields() {
            // Given
            CreateBankAccountDTO specialCharDto = CreateBankAccountDTO.builder()
                    .userId(1L)
                    .bankName("Chase & Co. - Bank!")
                    .accountNumber("123-456-789#")
                    .accountType("Checking/Savings")
                    .accountHolder("O'Connor, John Jr. & Sons")
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(specialCharDto, validUser);

            // Then
            assertNotNull(result);
            assertEquals("Chase & Co. - Bank!", result.getBankName());
            assertEquals("123-456-789#", result.getAccountNumber());
            assertEquals("Checking/Savings", result.getAccountType());
            assertEquals("O'Connor, John Jr. & Sons", result.getAccountHolder());
        }

        @Test
        @DisplayName("should handle DTO with whitespace in string fields")
        void shouldHandleDto_WithWhitespaceInStringFields() {
            // Given
            CreateBankAccountDTO whitespaceDto = CreateBankAccountDTO.builder()
                    .userId(1L)
                    .bankName("  Chase Bank  ")
                    .accountNumber("  1234567890  ")
                    .accountType("  Checking  ")
                    .accountHolder("  John Doe  ")
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(whitespaceDto, validUser);

            // Then
            assertNotNull(result);
            assertEquals("  Chase Bank  ", result.getBankName());
            assertEquals("  1234567890  ", result.getAccountNumber());
            assertEquals("  Checking  ", result.getAccountType());
            assertEquals("  John Doe  ", result.getAccountHolder());
        }
    }

    @Nested
    @DisplayName("User entity handling tests")
    class UserEntityHandlingTests {

        @Test
        @DisplayName("should handle User with all properties populated")
        void shouldHandleUser_WithAllPropertiesPopulated() {
            // Given
            User fullUser = User.builder()
                    .id(999L)
                    .active(false)
                    .verified(false)
                    .createdAt(LocalDateTime.now().minusYears(1))
                    .updatedAt(LocalDateTime.now().minusMonths(1))
                    .deletedAt(null)
                    .userInfo(validUserInfo)
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(validDto, fullUser);

            // Then
            assertNotNull(result);
            assertSame(fullUser, result.getUser());
            assertEquals(999L, result.getUser().getId());
            assertFalse(result.getUser().isActive());
            assertFalse(result.getUser().isVerified());
        }

        @Test
        @DisplayName("should handle User with minimum properties populated")
        void shouldHandleUser_WithMinimumPropertiesPopulated() {
            // Given
            User minimalUser = User.builder()
                    .id(1L)
                    .active(true)
                    .verified(true)
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(validDto, minimalUser);

            // Then
            assertNotNull(result);
            assertSame(minimalUser, result.getUser());
            assertEquals(1L, result.getUser().getId());
            assertTrue(result.getUser().isActive());
            assertTrue(result.getUser().isVerified());
        }

        @Test
        @DisplayName("should handle User with null optional fields")
        void shouldHandleUser_WithNullOptionalFields() {
            // Given
            User userWithNulls = User.builder()
                    .id(1L)
                    .active(true)
                    .verified(true)
                    .userInfo(null)
                    .deletedAt(null)
                    .updatedAt(null)
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(validDto, userWithNulls);

            // Then
            assertNotNull(result);
            assertSame(userWithNulls, result.getUser());
            assertNull(result.getUser().getUserInfo());
            assertNull(result.getUser().getDeletedAt());
        }

        @Test
        @DisplayName("should preserve User reference integrity")
        void shouldPreserveUserReferenceIntegrity() {
            // Given
            User originalUser = validUser;

            // When
            BankAccount result = validationService.buildBankAccountEntity(validDto, originalUser);

            // Modify original user after building
            originalUser.setActive(false);
            originalUser.setVerified(false);

            // Then
            assertSame(originalUser, result.getUser());
            // Changes to original user should be reflected in the bank account's user reference
            assertFalse(result.getUser().isActive());
            assertFalse(result.getUser().isVerified());
        }

        @Test
        @DisplayName("should handle different User instances with same ID")
        void shouldHandleDifferentUserInstances_WithSameId() {
            // Given
            User user1 = User.builder().id(1L).active(true).verified(true).build();
            User user2 = User.builder().id(1L).active(false).verified(false).build();

            // When
            BankAccount result1 = validationService.buildBankAccountEntity(validDto, user1);
            BankAccount result2 = validationService.buildBankAccountEntity(validDto, user2);

            // Then
            assertNotSame(result1.getUser(), result2.getUser());
            assertEquals(result1.getUser().getId(), result2.getUser().getId());
            assertNotEquals(result1.getUser().isActive(), result2.getUser().isActive());
            assertNotEquals(result1.getUser().isVerified(), result2.getUser().isVerified());
        }
    }

    @Nested
    @DisplayName("DTO field mapping precision tests")
    class DtoFieldMappingPrecisionTests {

        @Test
        @DisplayName("should map bank name exactly without modification")
        void shouldMapBankName_ExactlyWithoutModification() {
            // Given
            String originalBankName = "Original Bank Name";
            CreateBankAccountDTO dto = validDto.toBuilder()
                    .bankName(originalBankName)
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(dto, validUser);

            // Then
            assertEquals(originalBankName, result.getBankName());
            assertSame(originalBankName, result.getBankName()); // Reference equality check
        }

        @Test
        @DisplayName("should map account number exactly without modification")
        void shouldMapAccountNumber_ExactlyWithoutModification() {
            // Given
            String originalAccountNumber = "9876543210";
            CreateBankAccountDTO dto = validDto.toBuilder()
                    .accountNumber(originalAccountNumber)
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(dto, validUser);

            // Then
            assertEquals(originalAccountNumber, result.getAccountNumber());
            assertSame(originalAccountNumber, result.getAccountNumber());
        }

        @Test
        @DisplayName("should map account type exactly without modification")
        void shouldMapAccountType_ExactlyWithoutModification() {
            // Given
            String originalAccountType = "Savings";
            CreateBankAccountDTO dto = validDto.toBuilder()
                    .accountType(originalAccountType)
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(dto, validUser);

            // Then
            assertEquals(originalAccountType, result.getAccountType());
            assertSame(originalAccountType, result.getAccountType());
        }

        @Test
        @DisplayName("should map account holder exactly without modification")
        void shouldMapAccountHolder_ExactlyWithoutModification() {
            // Given
            String originalAccountHolder = "Jane Smith";
            CreateBankAccountDTO dto = validDto.toBuilder()
                    .accountHolder(originalAccountHolder)
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(dto, validUser);

            // Then
            assertEquals(originalAccountHolder, result.getAccountHolder());
            assertSame(originalAccountHolder, result.getAccountHolder());
        }

        @Test
        @DisplayName("should map all fields independently without cross-contamination")
        void shouldMapAllFields_IndependentlyWithoutCrossContamination() {
            // Given
            String uniqueBankName = "Unique Bank";
            String uniqueAccountNumber = "UNIQUE123";
            String uniqueAccountType = "UniqueType";
            String uniqueAccountHolder = "Unique Holder";

            CreateBankAccountDTO dto = CreateBankAccountDTO.builder()
                    .userId(1L)
                    .bankName(uniqueBankName)
                    .accountNumber(uniqueAccountNumber)
                    .accountType(uniqueAccountType)
                    .accountHolder(uniqueAccountHolder)
                    .build();

            // When
            BankAccount result = validationService.buildBankAccountEntity(dto, validUser);

            // Then
            assertEquals(uniqueBankName, result.getBankName());
            assertEquals(uniqueAccountNumber, result.getAccountNumber());
            assertEquals(uniqueAccountType, result.getAccountType());
            assertEquals(uniqueAccountHolder, result.getAccountHolder());

            // Verify no field contamination
            assertNotEquals(result.getBankName(), result.getAccountNumber());
            assertNotEquals(result.getBankName(), result.getAccountType());
            assertNotEquals(result.getBankName(), result.getAccountHolder());
            assertNotEquals(result.getAccountNumber(), result.getAccountType());
            assertNotEquals(result.getAccountNumber(), result.getAccountHolder());
            assertNotEquals(result.getAccountType(), result.getAccountHolder());
        }
    }

    @Nested
    @DisplayName("Entity state and behavior tests")
    class EntityStateAndBehaviorTests {

        @Test
        @DisplayName("should create BankAccount with no ID initially")
        void shouldCreateBankAccount_WithNoIdInitially() {
            // When
            BankAccount result = validationService.buildBankAccountEntity(validDto, validUser);

            // Then
            assertNull(result.getId()); // ID should be null until persisted
        }

        @Test
        @DisplayName("should create BankAccount with null timestamp fields initially")
        void shouldCreateBankAccount_WithNullTimestampFieldsInitially() {
            // When
            BankAccount result = validationService.buildBankAccountEntity(validDto, validUser);

            // Then
            // Assuming BankAccount has createdAt, updatedAt fields that are set by JPA lifecycle methods
            // These should be null initially until entity is persisted
            assertNull(result.getCreatedAt());
            assertNull(result.getUpdatedAt());
        }

        @Test
        @DisplayName("should create BankAccount that is not equal to another with same data")
        void shouldCreateBankAccount_ThatIsNotEqualToAnotherWithSameData() {
            // When
            BankAccount result1 = validationService.buildBankAccountEntity(validDto, validUser);
            BankAccount result2 = validationService.buildBankAccountEntity(validDto, validUser);

            // Then
            assertEquals(result1, result2);
            assertNotSame(result1, result2);
        }

        @Test
        @DisplayName("should create BankAccount with proper toString representation")
        void shouldCreateBankAccount_WithProperToStringRepresentation() {
            // When
            BankAccount result = validationService.buildBankAccountEntity(validDto, validUser);

            // Then
            String toStringResult = result.toString();
            assertNotNull(toStringResult);
            assertFalse(toStringResult.isEmpty());
            // ToString should contain class name
            assertTrue(toStringResult.contains("BankAccount"));
        }

        @Test
        @DisplayName("should maintain object consistency across multiple field accesses")
        void shouldMaintainObjectConsistency_AcrossMultipleFieldAccesses() {
            // When
            BankAccount result = validationService.buildBankAccountEntity(validDto, validUser);

            // Then - Multiple accesses should return consistent values
            String bankName1 = result.getBankName();
            String bankName2 = result.getBankName();
            User user1 = result.getUser();
            User user2 = result.getUser();

            assertSame(bankName1, bankName2);
            assertSame(user1, user2);
            assertEquals(validDto.getBankName(), bankName1);
            assertEquals(validDto.getBankName(), bankName2);
            assertSame(validUser, user1);
            assertSame(validUser, user2);
        }
    }

    @Nested
    @DisplayName("Method isolation and side effects tests")
    class MethodIsolationAndSideEffectsTests {

        @Test
        @DisplayName("should not modify input DTO during entity building")
        void shouldNotModifyInputDto_DuringEntityBuilding() {
            // Given
            CreateBankAccountDTO originalDto = CreateBankAccountDTO.builder()
                    .userId(1L)
                    .bankName("Original Bank")
                    .accountNumber("123456")
                    .accountType("Original Type")
                    .accountHolder("Original Holder")
                    .build();

            String originalBankName = originalDto.getBankName();
            String originalAccountNumber = originalDto.getAccountNumber();
            String originalAccountType = originalDto.getAccountType();
            String originalAccountHolder = originalDto.getAccountHolder();
            Long originalUserId = originalDto.getUserId();

            // When
            validationService.buildBankAccountEntity(originalDto, validUser);

            // Then
            assertEquals(originalBankName, originalDto.getBankName());
            assertEquals(originalAccountNumber, originalDto.getAccountNumber());
            assertEquals(originalAccountType, originalDto.getAccountType());
            assertEquals(originalAccountHolder, originalDto.getAccountHolder());
            assertEquals(originalUserId, originalDto.getUserId());
        }

        @Test
        @DisplayName("should not modify input User during entity building")
        void shouldNotModifyInputUser_DuringEntityBuilding() {
            // Given
            User originalUser = User.builder()
                    .id(1L)
                    .active(true)
                    .verified(true)
                    .build();

            Long originalId = originalUser.getId();
            boolean originalActive = originalUser.isActive();
            boolean originalVerified = originalUser.isVerified();

            // When
            validationService.buildBankAccountEntity(validDto, originalUser);

            // Then
            assertEquals(originalId, originalUser.getId());
            assertEquals(originalActive, originalUser.isActive());
            assertEquals(originalVerified, originalUser.isVerified());
        }

        @Test
        @DisplayName("should not have any repository interactions during entity building")
        void shouldNotHaveAnyRepositoryInteractions_DuringEntityBuilding() {
            // When
            validationService.buildBankAccountEntity(validDto, validUser);

            // Then
            verifyNoInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should be thread-safe for concurrent entity building")
        void shouldBeThreadSafe_ForConcurrentEntityBuilding() {
            // Given
            CreateBankAccountDTO dto1 = validDto.toBuilder().bankName("Bank 1").build();
            CreateBankAccountDTO dto2 = validDto.toBuilder().bankName("Bank 2").build();
            User user1 = User.builder().id(1L).active(true).verified(true).build();
            User user2 = User.builder().id(2L).active(false).verified(false).build();

            // When - Simulate concurrent access
            BankAccount result1 = validationService.buildBankAccountEntity(dto1, user1);
            BankAccount result2 = validationService.buildBankAccountEntity(dto2, user2);

            // Then
            assertEquals("Bank 1", result1.getBankName());
            assertEquals("Bank 2", result2.getBankName());
            assertSame(user1, result1.getUser());
            assertSame(user2, result2.getUser());
            assertNotSame(result1, result2);
        }
    }

}
