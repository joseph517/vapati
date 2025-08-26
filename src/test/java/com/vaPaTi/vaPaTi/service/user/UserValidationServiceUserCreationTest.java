package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.dtos.CreateUserDTO;
import com.vaPaTi.vaPaTi.dtos.CreateUserInfoDTO;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.UserInfoMapper;
import com.vaPaTi.vaPaTi.repository.UserInfoRepository;
import com.vaPaTi.vaPaTi.validation.UserValidationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("UserValidationService - User Creation Tests")
class UserValidationServiceUserCreationTest {

    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserValidationService userValidationService;

    private CreateUserDTO createUserDTO;
    private CreateUserInfoDTO createUserInfoDTO;
    private UserInfo mappedUserInfo;
    private final String encodedPassword = "encodedPassword123";

    @BeforeEach
    void setUp() {
        createUserDTO = CreateUserDTO.builder()
                .categoryIds(List.of(1L, 2L, 3L))
                .build();

        createUserInfoDTO = CreateUserInfoDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .userName("johndoe123")
                .password("Password123!")
                .phone("+1234567890")
                .description("Test user description")
                .profilePicture("profile.jpg")
                .build();

        mappedUserInfo = UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .userName("johndoe123")
                .password("Password123")
                .phone("+1234567890")
                .description("Test user description")
                .profilePicture("profile.jpg")
                .build();
    }

    @Nested
    @DisplayName("createAndSetupUser Tests")
    class CreateAndSetupUserTests {

        @Test
        @DisplayName("Should create user with correct default values when DTO is provided")
        void shouldCreateUserWithCorrectDefaultValues_WhenDTOProvided() {
            // Given
            LocalDateTime beforeExecution = LocalDateTime.now().minusSeconds(1);

            // When
            User result = userValidationService.createAndSetupUser(createUserDTO);

            // Then
            LocalDateTime afterExecution = LocalDateTime.now().plusSeconds(1);

            assertNotNull(result);
            assertTrue(result.isActive(), "User should be active by default");
            assertFalse(result.isVerified(), "User should not be verified by default");
            assertNotNull(result.getCreatedAt(), "CreatedAt should be set");
            assertNotNull(result.getUpdatedAt(), "UpdatedAt should be set");

            assertTrue(result.getCreatedAt().isAfter(beforeExecution) &&
                            result.getCreatedAt().isBefore(afterExecution),
                    "CreatedAt should be set to current time");
            assertTrue(result.getUpdatedAt().isAfter(beforeExecution) &&
                            result.getUpdatedAt().isBefore(afterExecution),
                    "UpdatedAt should be set to current time");
        }

        @Test
        @DisplayName("Should create user with empty category list when DTO has empty categories")
        void shouldCreateUser_WhenDTOHasEmptyCategories() {
            // Given
            CreateUserDTO dtoWithEmptyCategories = CreateUserDTO.builder()
                    .categoryIds(new ArrayList<>())
                    .build();

            // When
            User result = userValidationService.createAndSetupUser(dtoWithEmptyCategories);

            // Then
            assertNotNull(result);
            assertTrue(result.isActive());
            assertFalse(result.isVerified());
        }

        @Test
        @DisplayName("Should throw MessageException when DTO is null")
        void shouldThrowMessageException_WhenDTOIsNull() {
            // When & Then
            assertThrows(MessageException.class,
                    () -> userValidationService.createAndSetupUser(null),
                    "Should throw MessageException when DTO is null");
        }
    }

    @Nested
    @DisplayName("createUserInfo Tests")
    class CreateUserInfoTests {

        @BeforeEach
        void setUpMocks() {
            lenient().when(userInfoRepository.existsByEmailAndUserIdNot(anyString(), anyLong())).thenReturn(false);
            lenient().when(userInfoRepository.existsByUserNameAndUserIdNot(anyString(), anyLong())).thenReturn(false);
            lenient().when(userInfoMapper.fromCreateUserInfoDTO(any(CreateUserInfoDTO.class))).thenReturn(mappedUserInfo);
            lenient().when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        }

        @Test
        @DisplayName("Should create UserInfo successfully with valid data")
        void shouldCreateUserInfoSuccessfully_WhenValidDataProvided() {
            // Given
            LocalDateTime beforeExecution = LocalDateTime.now().minusSeconds(1);

            // When
            UserInfo result = userValidationService.createUserInfo(createUserInfoDTO);

            // Then
            LocalDateTime afterExecution = LocalDateTime.now().plusSeconds(1);

            assertNotNull(result);
            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
            assertEquals("john.doe@example.com", result.getEmail());
            assertEquals("johndoe123", result.getUserName());
            assertEquals(encodedPassword, result.getPassword());
            assertEquals("+1234567890", result.getPhone());
            assertEquals("Test user description", result.getDescription());
            assertEquals("profile.jpg", result.getProfilePicture());

            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
            assertTrue(result.getCreatedAt().isAfter(beforeExecution) &&
                    result.getCreatedAt().isBefore(afterExecution));
            assertTrue(result.getUpdatedAt().isAfter(beforeExecution) &&
                    result.getUpdatedAt().isBefore(afterExecution));

            InOrder inOrder = inOrder(userInfoRepository, userInfoMapper, passwordEncoder);

            inOrder.verify(userInfoRepository).existsByUserNameAndUserIdNot("johndoe123", -1L);
            inOrder.verify(userInfoMapper).fromCreateUserInfoDTO(createUserInfoDTO);
            inOrder.verify(passwordEncoder).encode("Password123!");
        }

        @Test
        @DisplayName("Should throw MessageException when DTO is null")
        void shouldThrowMessageException_WhenDTOIsNull() {
            // When & Then
            assertThrows(MessageException.class,
                    () -> userValidationService.createUserInfo(null),
                    "Should throw MessageException when DTO is null");
        }

        @Nested
        @DisplayName("Email Validation Tests")
        class EmailValidationTests {

            @Test
            @DisplayName("Should throw MessageException when email format is invalid")
            void shouldThrowMessageException_WhenEmailFormatInvalid() {
                // Given
                createUserInfoDTO.setEmail("invalid-email");

                // When & Then
                MessageException exception = assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                assertEquals("Invalid email format", exception.getMessage());
                verify(userInfoRepository, never()).existsByEmailAndUserIdNot(anyString(), anyLong());
            }


            @Test
            @DisplayName("Should throw MessageException when email already exists")
            void shouldThrowMessageException_WhenEmailAlreadyExists() {
                // Given
                when(userInfoRepository.existsByEmailAndUserIdNot("john.doe@example.com", -1L))
                        .thenReturn(true);

                // When & Then
                MessageException exception = assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                assertEquals("Email already exists", exception.getMessage());
                verify(userInfoRepository).existsByEmailAndUserIdNot("john.doe@example.com", -1L);
                verify(userInfoRepository, never()).existsByUserNameAndUserIdNot(anyString(), anyLong());
            }

            @Test
            @DisplayName("Should validate email with various valid formats")
            void shouldValidateEmail_WithVariousValidFormats() {
                // Given
                String[] validEmails = {
                        "test@example.com",
                        "user.name@domain.co.uk",
                        "user+tag@example.org",
                        "user123@test-domain.com",
                        "a@b.co"
                };

                createUserInfoDTO.setPassword("Valid@123");
                createUserInfoDTO.setUserName("testuser");

                // When & Then
                for (String email : validEmails) {
                    createUserInfoDTO.setEmail(email);
                    assertDoesNotThrow(() -> userValidationService.createUserInfo(createUserInfoDTO),
                            "Should accept valid email: " + email);
                }
            }

            @Test
            @DisplayName("Should reject email with various invalid formats")
            void shouldRejectEmail_WithVariousInvalidFormats() {
                // Given
                String[] invalidEmails = {
                        "invalid",
                        "@example.com",
                        "user@",
                        "user@domain",
                        "user.domain.com",
                        "user@domain.",
                        "user@@domain.com",
                        ""
                };

                // When & Then
                for (String email : invalidEmails) {
                    createUserInfoDTO.setEmail(email);
                    MessageException exception = assertThrows(MessageException.class,
                            () -> userValidationService.createUserInfo(createUserInfoDTO),
                            "Should reject invalid email: " + email);
                    assertEquals("Invalid email format", exception.getMessage());
                }
            }
        }

        @Nested
        @DisplayName("Username Validation Tests")
        class UsernameValidationTests {

            @Test
            @DisplayName("Should throw MessageException when username is too short")
            void shouldThrowMessageException_WhenUsernameTooShort() {
                // Given
                createUserInfoDTO.setUserName("ab");

                // When & Then
                MessageException exception = assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                assertEquals("Username must be between 3 and 50 characters", exception.getMessage());
            }

            @Test
            @DisplayName("Should throw MessageException when username is too long")
            void shouldThrowMessageException_WhenUsernameTooLong() {
                // Given
                String longUsername = "a".repeat(51);
                createUserInfoDTO.setUserName(longUsername);

                // When & Then
                MessageException exception = assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                assertEquals("Username must be between 3 and 50 characters", exception.getMessage());
            }

            @Test
            @DisplayName("Should throw MessageException when username already exists")
            void shouldThrowMessageException_WhenUsernameAlreadyExists() {
                // Given
                when(userInfoRepository.existsByUserNameAndUserIdNot("johndoe123", -1L))
                        .thenReturn(true);

                // When & Then
                MessageException exception = assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                assertEquals("Username already exists", exception.getMessage());
                verify(userInfoRepository).existsByUserNameAndUserIdNot("johndoe123", -1L);
            }

            @Test
            @DisplayName("Should accept username with minimum valid length")
            void shouldAcceptUsername_WithMinimumValidLength() {
                // Given
                createUserInfoDTO.setUserName("abc");
                createUserInfoDTO.setPassword("Password123!");
                createUserInfoDTO.setEmail("user@example.com");

                // When & Then
                assertDoesNotThrow(() -> userValidationService.createUserInfo(createUserInfoDTO));
            }

            @Test
            @DisplayName("Should accept username with maximum valid length")
            void shouldAcceptUsername_WithMaximumValidLength() {
                // Given
                String maxLengthUsername = "a".repeat(50);
                createUserInfoDTO.setUserName(maxLengthUsername);

                // When & Then
                assertDoesNotThrow(() -> userValidationService.createUserInfo(createUserInfoDTO));
            }
        }

        @Nested
        @DisplayName("Password Validation Tests")
        class PasswordValidationTests {

            @Test
            @DisplayName("Should throw MessageException when password is too short")
            void shouldThrowMessageException_WhenPasswordTooShort() {
                // Given
                createUserInfoDTO.setPassword("1234");

                // When & Then
                MessageException exception = assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                assertEquals("Password must be at least 8 characters long", exception.getMessage());
            }

            @Test
            @DisplayName("Should throw MessageException when password lacks uppercase letter")
            void shouldThrowMessageException_WhenPasswordLacksUppercase() {
                // Given
                createUserInfoDTO.setPassword("password1234!");

                // When & Then
                MessageException exception = assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                assertEquals("Password must contain at least one uppercase letter", exception.getMessage());
            }

            @Test
            @DisplayName("Should throw MessageException when password lacks lowercase letter")
            void shouldThrowMessageException_WhenPasswordLacksLowercase() {
                // Given
                createUserInfoDTO.setPassword("PASSWORD123!");

                // When & Then
                MessageException exception = assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                assertEquals("Password must contain at least one lowercase letter", exception.getMessage());
            }

            @Test
            @DisplayName("Should throw MessageException when password lacks number")
            void shouldThrowMessageException_WhenPasswordLacksNumber() {
                // Given
                createUserInfoDTO.setPassword("Password!");

                // When & Then
                MessageException exception = assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                assertEquals("Password must contain at least one number", exception.getMessage());
            }

            @Test
            @DisplayName("Should accept password with all required criteria")
            void shouldAcceptPassword_WithAllRequiredCriteria() {
                // Given
                String[] validPasswords = {
                        "Password123!",
                        "MySecure1Pass!",
                        "Test123Password!",
                        "Valid1Password!"
                };

                // When & Then
                for (String password : validPasswords) {
                    createUserInfoDTO.setPassword(password);
                    assertDoesNotThrow(() -> userValidationService.createUserInfo(createUserInfoDTO),
                            "Should accept valid password: " + password);
                }
            }
        }

        @Nested
        @DisplayName("Integration and Edge Cases")
        class IntegrationAndEdgeCaseTests {

            @Test
            @DisplayName("Should handle mapper returning null")
            void shouldHandleMapperReturningNull() {
                // Given
                when(userInfoMapper.fromCreateUserInfoDTO(createUserInfoDTO)).thenReturn(null);

                // When & Then
                assertThrows(NullPointerException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                verify(userInfoMapper).fromCreateUserInfoDTO(createUserInfoDTO);
                verify(passwordEncoder, never()).encode(anyString());
            }

            @Test
            @DisplayName("Should verify all validations are called in correct order")
            void shouldVerifyValidationsCalledInCorrectOrder() {
                // When
                userValidationService.createUserInfo(createUserInfoDTO);

                // Then
                InOrder inOrder = inOrder(userInfoRepository);
                inOrder.verify(userInfoRepository).existsByEmailAndUserIdNot("john.doe@example.com", -1L);
                inOrder.verify(userInfoRepository).existsByUserNameAndUserIdNot("johndoe123", -1L);
            }

            @Test
            @DisplayName("Should not call mapper if email validation fails")
            void shouldNotCallMapper_WhenEmailValidationFails() {
                // Given
                createUserInfoDTO.setEmail("invalid-email");

                // When & Then
                assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                verify(userInfoMapper, never()).fromCreateUserInfoDTO(any());
                verify(passwordEncoder, never()).encode(anyString());
            }

            @Test
            @DisplayName("Should not call mapper if username validation fails")
            void shouldNotCallMapper_WhenUsernameValidationFails() {
                // Given
                createUserInfoDTO.setUserName("ab");

                // When & Then
                assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                verify(userInfoMapper, never()).fromCreateUserInfoDTO(any());
                verify(passwordEncoder, never()).encode(anyString());
            }

            @Test
            @DisplayName("Should not call mapper if password validation fails")
            void shouldNotCallMapper_WhenPasswordValidationFails() {
                // Given
                createUserInfoDTO.setPassword("weak");

                // When & Then
                assertThrows(MessageException.class,
                        () -> userValidationService.createUserInfo(createUserInfoDTO));

                verify(userInfoMapper, never()).fromCreateUserInfoDTO(any());
                verify(passwordEncoder, never()).encode(anyString());
            }

            @Test
            @DisplayName("Should encode password only after successful mapping")
            void shouldEncodePassword_OnlyAfterSuccessfulMapping() {
                // When
                UserInfo result = userValidationService.createUserInfo(createUserInfoDTO);

                // Then
                InOrder inOrder = inOrder(userInfoMapper, passwordEncoder);
                inOrder.verify(userInfoMapper).fromCreateUserInfoDTO(createUserInfoDTO);
                inOrder.verify(passwordEncoder).encode("Password123!");

                assertEquals(encodedPassword, result.getPassword());
            }

            @Test
            @DisplayName("Should preserve original password in DTO after encoding")
            void shouldPreserveOriginalPasswordInDTO_AfterEncoding() {
                // Given
                String originalPassword = createUserInfoDTO.getPassword();

                // When
                userValidationService.createUserInfo(createUserInfoDTO);

                // Then
                assertEquals(originalPassword, createUserInfoDTO.getPassword(),
                        "Original DTO password should not be modified");
                verify(passwordEncoder).encode(originalPassword);
            }
        }
    }
}
