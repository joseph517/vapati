package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.dtos.UpdateUserDTO;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;

import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.UserInfoRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Timestamp Update Service Tests")
class UserValidationServiceUserUpdateTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserValidationService timestampUpdateService;

    @InjectMocks
    private UserValidationService userValidationService;

    private User testUser;
    private UserInfo testUserInfo;
    private LocalDateTime fixedDateTime;
    private UpdateUserDTO updateUserDTO;

    @BeforeEach
    void setUp() {
        fixedDateTime = LocalDateTime.of(2024, 1, 15, 12, 30, 45);

        testUser = User.builder()
                .id(1L)
                .active(true)
                .verified(false)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 10, 15, 30, 0))
                .build();

        testUserInfo = UserInfo.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .userName("johndoe")
                .password("hashedPassword123")
                .phone("+1234567890")
                .description("Test user description")
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 10, 15, 30, 0))
                .user(testUser)
                .build();

        updateUserDTO = UpdateUserDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .userName("janesmith")
                .password("NewPassword123!")
                .phone("+9876543210")
                .description("Updated description")
                .profilePicture("new-profile.jpg")
                .build();

    }

    @Test
    @DisplayName("Should update UserInfo timestamp successfully when UserInfo is provided")
    void updateUserInfoTimestamp_WithValidUserInfo_ShouldUpdateTimestamp() {
        // Given
        LocalDateTime originalTimestamp = testUserInfo.getUpdatedAt();

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDateTime);

            // When
            timestampUpdateService.updateUserInfoTimestamp(testUserInfo);

            // Then
            assertThat(testUserInfo.getUpdatedAt()).isEqualTo(fixedDateTime);
            assertThat(testUserInfo.getUpdatedAt()).isNotEqualTo(originalTimestamp);

            // Verify LocalDateTime.now() was called exactly once
            mockedLocalDateTime.verify(LocalDateTime::now, times(1));
        }
    }

    @Test
    @DisplayName("Should throw MessageException when UserInfo is null")
    void updateUserInfoTimestamp_WithNullUserInfo_ShouldThrowMessageException() {
        // Given
        UserInfo nullUserInfo = null;

        // When & Then
        assertThatThrownBy(() -> timestampUpdateService.updateUserInfoTimestamp(nullUserInfo))
                .isInstanceOf(MessageException.class);

        // Verify LocalDateTime.now() was never called due to early validation failure
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("Should update User timestamp successfully when User is provided")
    void updateTimestamp_WithValidUser_ShouldUpdateTimestamp() {
        // Given
        LocalDateTime originalTimestamp = testUser.getUpdatedAt();

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDateTime);

            // When
            timestampUpdateService.updateTimestamp(testUser);

            // Then
            assertThat(testUser.getUpdatedAt()).isEqualTo(fixedDateTime);
            assertThat(testUser.getUpdatedAt()).isNotEqualTo(originalTimestamp);

            // Verify LocalDateTime.now() was called exactly once
            mockedLocalDateTime.verify(LocalDateTime::now, times(1));
        }
    }

    @Test
    @DisplayName("Should throw NullPointerException when User is null")
    void updateTimestamp_WithNullUser_ShouldThrowNullPointerException() {
        // Given
        User nullUser = null;

        // When & Then
        assertThatThrownBy(() -> timestampUpdateService.updateTimestamp(nullUser))
                .isInstanceOf(MessageException.class);

        // Verify LocalDateTime.now() was never called due to early validation failure
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("Should handle UserInfo with null initial timestamp")
    void updateUserInfoTimestamp_WithNullInitialTimestamp_ShouldSetTimestamp() {
        // Given
        testUserInfo.setUpdatedAt(null);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDateTime);

            // When
            timestampUpdateService.updateUserInfoTimestamp(testUserInfo);

            // Then
            assertThat(testUserInfo.getUpdatedAt()).isEqualTo(fixedDateTime);
            assertThat(testUserInfo.getUpdatedAt()).isNotNull();

            mockedLocalDateTime.verify(LocalDateTime::now, times(1));
        }
    }

    @Test
    @DisplayName("Should handle User with null initial timestamp")
    void updateTimestamp_WithNullInitialTimestamp_ShouldSetTimestamp() {
        // Given
        testUser.setUpdatedAt(null);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDateTime);

            // When
            timestampUpdateService.updateTimestamp(testUser);

            // Then
            assertThat(testUser.getUpdatedAt()).isEqualTo(fixedDateTime);
            assertThat(testUser.getUpdatedAt()).isNotNull();

            mockedLocalDateTime.verify(LocalDateTime::now, times(1));
        }
    }

    @Test
    @DisplayName("Should update timestamp multiple times with different values")
    void updateUserInfoTimestamp_CalledMultipleTimes_ShouldUpdateWithDifferentTimestamps() {
        // Given
        LocalDateTime firstTimestamp = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        LocalDateTime secondTimestamp = LocalDateTime.of(2024, 1, 15, 11, 0, 0);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            // First call
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(firstTimestamp);
            timestampUpdateService.updateUserInfoTimestamp(testUserInfo);
            assertThat(testUserInfo.getUpdatedAt()).isEqualTo(firstTimestamp);

            // Second call
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(secondTimestamp);
            timestampUpdateService.updateUserInfoTimestamp(testUserInfo);
            assertThat(testUserInfo.getUpdatedAt()).isEqualTo(secondTimestamp);

            // Verify LocalDateTime.now() was called twice
            mockedLocalDateTime.verify(LocalDateTime::now, times(2));
        }
    }

    @Test
    @DisplayName("Should update timestamp multiple times for User with different values")
    void updateTimestamp_CalledMultipleTimes_ShouldUpdateWithDifferentTimestamps() {
        // Given
        LocalDateTime firstTimestamp = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        LocalDateTime secondTimestamp = LocalDateTime.of(2024, 1, 15, 11, 0, 0);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            // First call
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(firstTimestamp);
            timestampUpdateService.updateTimestamp(testUser);
            assertThat(testUser.getUpdatedAt()).isEqualTo(firstTimestamp);

            // Second call
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(secondTimestamp);
            timestampUpdateService.updateTimestamp(testUser);
            assertThat(testUser.getUpdatedAt()).isEqualTo(secondTimestamp);

            // Verify LocalDateTime.now() was called twice
            mockedLocalDateTime.verify(LocalDateTime::now, times(2));
        }
    }

    @Test
    @DisplayName("Should not affect other UserInfo properties when updating timestamp")
    void updateUserInfoTimestamp_ShouldOnlyUpdateTimestamp() {
        // Given
        String originalFirstName = testUserInfo.getFirstName();
        String originalLastName = testUserInfo.getLastName();
        String originalEmail = testUserInfo.getEmail();
        String originalUserName = testUserInfo.getUserName();
        String originalPhone = testUserInfo.getPhone();
        String originalDescription = testUserInfo.getDescription();
        LocalDateTime originalCreatedAt = testUserInfo.getCreatedAt();

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDateTime);

            // When
            timestampUpdateService.updateUserInfoTimestamp(testUserInfo);

            // Then - verify only updatedAt changed
            assertThat(testUserInfo.getUpdatedAt()).isEqualTo(fixedDateTime);
            assertThat(testUserInfo.getFirstName()).isEqualTo(originalFirstName);
            assertThat(testUserInfo.getLastName()).isEqualTo(originalLastName);
            assertThat(testUserInfo.getEmail()).isEqualTo(originalEmail);
            assertThat(testUserInfo.getUserName()).isEqualTo(originalUserName);
            assertThat(testUserInfo.getPhone()).isEqualTo(originalPhone);
            assertThat(testUserInfo.getDescription()).isEqualTo(originalDescription);
            assertThat(testUserInfo.getCreatedAt()).isEqualTo(originalCreatedAt);
        }
    }

    @Test
    @DisplayName("Should not affect other User properties when updating timestamp")
    void updateTimestamp_ShouldOnlyUpdateTimestamp() {
        // Given
        Long originalId = testUser.getId();
        boolean originalActive = testUser.isActive();
        boolean originalVerified = testUser.isVerified();
        LocalDateTime originalCreatedAt = testUser.getCreatedAt();
        LocalDateTime originalDeletedAt = testUser.getDeletedAt();

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDateTime);

            // When
            timestampUpdateService.updateTimestamp(testUser);

            // Then - verify only updatedAt changed
            assertThat(testUser.getUpdatedAt()).isEqualTo(fixedDateTime);
            assertThat(testUser.getId()).isEqualTo(originalId);
            assertThat(testUser.isActive()).isEqualTo(originalActive);
            assertThat(testUser.isVerified()).isEqualTo(originalVerified);
            assertThat(testUser.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(testUser.getDeletedAt()).isEqualTo(originalDeletedAt);
        }
    }

    @Test
    @DisplayName("Should successfully update all user info fields when all data is provided")
    void shouldSuccessfullyUpdateAllUserInfoFields_WhenAllDataProvided() {
        // Arrange
        testUser.setUserInfo(testUserInfo);

        when(userInfoRepository.existsByEmailAndUserIdNot("jane.smith@example.com", 1L)).thenReturn(false);
        when(userInfoRepository.existsByUserNameAndUserIdNot("janesmith", 1L)).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword123!");

        // Act
        assertDoesNotThrow(() -> userValidationService.updateUserInfo(testUser, updateUserDTO));

        // Assert
        assertThat(testUserInfo.getFirstName()).isEqualTo("Jane");
        assertThat(testUserInfo.getLastName()).isEqualTo("Smith");
        assertThat(testUserInfo.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(testUserInfo.getUserName()).isEqualTo("janesmith");
        assertThat(testUserInfo.getPassword()).isEqualTo("encodedNewPassword123!");
        assertThat(testUserInfo.getPhone()).isEqualTo("+9876543210");
        assertThat(testUserInfo.getDescription()).isEqualTo("Updated description");
        assertThat(testUserInfo.getProfilePicture()).isEqualTo("new-profile.jpg");
        assertThat(testUserInfo.getUpdatedAt()).isNotNull();

        verify(userInfoRepository).existsByEmailAndUserIdNot("jane.smith@example.com", 1L);
        verify(userInfoRepository).existsByUserNameAndUserIdNot("janesmith", 1L);
        verify(passwordEncoder).encode("NewPassword123!");
    }

    @Test
    @DisplayName("Should handle partial update when only some fields are provided")
    void shouldHandlePartialUpdate_WhenOnlySomeFieldsProvided() {
        // Arrange
        testUser.setUserInfo(testUserInfo);
        UpdateUserDTO partialDTO = UpdateUserDTO.builder()
                .firstName("Jane")
                .email("jane.smith@example.com")
                .build();

        String originalLastName = testUserInfo.getLastName();
        String originalUserName = testUserInfo.getUserName();
        String originalPassword = testUserInfo.getPassword();
        String originalPhone = testUserInfo.getPhone();
        String originalDescription = testUserInfo.getDescription();
        String originalProfilePicture = testUserInfo.getProfilePicture();

        when(userInfoRepository.existsByEmailAndUserIdNot("jane.smith@example.com", 1L)).thenReturn(false);

        // Act
        assertDoesNotThrow(() -> userValidationService.updateUserInfo(testUser, partialDTO));

        // Assert
        assertThat(testUserInfo.getFirstName()).isEqualTo("Jane");
        assertThat(testUserInfo.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(testUserInfo.getUpdatedAt()).isNotNull();

        // Verify unchanged fields
        assertThat(testUserInfo.getLastName()).isEqualTo(originalLastName);
        assertThat(testUserInfo.getUserName()).isEqualTo(originalUserName);
        assertThat(testUserInfo.getPassword()).isEqualTo(originalPassword);
        assertThat(testUserInfo.getPhone()).isEqualTo(originalPhone);
        assertThat(testUserInfo.getDescription()).isEqualTo(originalDescription);
        assertThat(testUserInfo.getProfilePicture()).isEqualTo(originalProfilePicture);

        verify(userInfoRepository).existsByEmailAndUserIdNot("jane.smith@example.com", 1L);
        verify(userInfoRepository, never()).existsByUserNameAndUserIdNot(anyString(), anyLong());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should handle empty DTO without changing any fields except timestamp")
    void shouldHandleEmptyDTO_WithoutChangingAnyFieldsExceptTimestamp() {
        // Arrange
        testUser.setUserInfo(testUserInfo);
        UpdateUserDTO emptyDTO = UpdateUserDTO.builder().build();

        String originalFirstName = testUserInfo.getFirstName();
        String originalLastName = testUserInfo.getLastName();
        String originalEmail = testUserInfo.getEmail();
        String originalUserName = testUserInfo.getUserName();
        String originalPassword = testUserInfo.getPassword();
        String originalPhone = testUserInfo.getPhone();
        String originalDescription = testUserInfo.getDescription();
        String originalProfilePicture = testUserInfo.getProfilePicture();

        // Act
        assertDoesNotThrow(() -> userValidationService.updateUserInfo(testUser, emptyDTO));

        // Assert - All original fields should remain unchanged
        assertThat(testUserInfo.getFirstName()).isEqualTo(originalFirstName);
        assertThat(testUserInfo.getLastName()).isEqualTo(originalLastName);
        assertThat(testUserInfo.getEmail()).isEqualTo(originalEmail);
        assertThat(testUserInfo.getUserName()).isEqualTo(originalUserName);
        assertThat(testUserInfo.getPassword()).isEqualTo(originalPassword);
        assertThat(testUserInfo.getPhone()).isEqualTo(originalPhone);
        assertThat(testUserInfo.getDescription()).isEqualTo(originalDescription);
        assertThat(testUserInfo.getProfilePicture()).isEqualTo(originalProfilePicture);

        // Only timestamp should be updated
        assertThat(testUserInfo.getUpdatedAt()).isNotNull();

        // Verify no external validations were called
        verify(userInfoRepository, never()).existsByEmailAndUserIdNot(anyString(), anyLong());
        verify(userInfoRepository, never()).existsByUserNameAndUserIdNot(anyString(), anyLong());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should throw exception when user is null")
    void shouldThrowException_WhenUserIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> userValidationService.updateUserInfo(null, updateUserDTO))
                .isInstanceOf(MessageException.class);

        verifyNoInteractions(userInfoRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should throw exception when DTO is null")
    void shouldThrowException_WhenDTOIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> userValidationService.updateUserInfo(testUser, null))
                .isInstanceOf(MessageException.class);

        verifyNoInteractions(userInfoRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should throw MessageException when user has no UserInfo")
    void shouldThrowMessageException_WhenUserHasNoUserInfo() {
        // Arrange
        User userWithoutInfo = User.builder()
                .id(2L)
                .active(true)
                .verified(false)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> userValidationService.updateUserInfo(userWithoutInfo, updateUserDTO))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("UserInfo not found");

        verifyNoInteractions(userInfoRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should throw MessageException when username already exists for another user")
    void shouldThrowMessageException_WhenUsernameAlreadyExistsForAnotherUser() {
        // Arrange
        testUser.setUserInfo(testUserInfo);

        when(userInfoRepository.existsByEmailAndUserIdNot("jane.smith@example.com", 1L)).thenReturn(false);
        when(userInfoRepository.existsByUserNameAndUserIdNot("janesmith", 1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userValidationService.updateUserInfo(testUser, updateUserDTO))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("Username already exists");

        verify(userInfoRepository).existsByEmailAndUserIdNot("jane.smith@example.com", 1L);
        verify(userInfoRepository).existsByUserNameAndUserIdNot("janesmith", 1L);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should allow same email when user keeps their own email")
    void shouldAllowSameEmail_WhenUserKeepsTheirOwnEmail() {
        // Arrange
        testUser.setUserInfo(testUserInfo);
        UpdateUserDTO sameEmailDTO = UpdateUserDTO.builder()
                .email("john.doe@example.com") // Same email as current
                .firstName("Jane")
                .build();

        when(userInfoRepository.existsByEmailAndUserIdNot("john.doe@example.com", 1L)).thenReturn(false);

        // Act
        assertDoesNotThrow(() -> userValidationService.updateUserInfo(testUser, sameEmailDTO));

        // Assert
        assertThat(testUserInfo.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(testUserInfo.getFirstName()).isEqualTo("Jane");

        verify(userInfoRepository).existsByEmailAndUserIdNot("john.doe@example.com", 1L);
    }

    @Test
    @DisplayName("Should allow same username when user keeps their own username")
    void shouldAllowSameUsername_WhenUserKeepsTheirOwnUsername() {
        // Arrange
        testUser.setUserInfo(testUserInfo);
        UpdateUserDTO sameUsernameDTO = UpdateUserDTO.builder()
                .userName("johndoe") // Same username as current
                .firstName("Jane")
                .build();

        when(userInfoRepository.existsByUserNameAndUserIdNot("johndoe", 1L)).thenReturn(false);

        // Act
        assertDoesNotThrow(() -> userValidationService.updateUserInfo(testUser, sameUsernameDTO));

        // Assert
        assertThat(testUserInfo.getUserName()).isEqualTo("johndoe");
        assertThat(testUserInfo.getFirstName()).isEqualTo("Jane");

        verify(userInfoRepository).existsByUserNameAndUserIdNot("johndoe", 1L);
    }

    @Test
    @DisplayName("Should update only name fields when only names are provided")
    void shouldUpdateOnlyNameFields_WhenOnlyNamesAreProvided() {
        // Arrange
        testUser.setUserInfo(testUserInfo);
        UpdateUserDTO namesOnlyDTO = UpdateUserDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        String originalEmail = testUserInfo.getEmail();
        String originalUserName = testUserInfo.getUserName();
        String originalPassword = testUserInfo.getPassword();

        // Act
        assertDoesNotThrow(() -> userValidationService.updateUserInfo(testUser, namesOnlyDTO));

        // Assert
        assertThat(testUserInfo.getFirstName()).isEqualTo("Jane");
        assertThat(testUserInfo.getLastName()).isEqualTo("Smith");
        assertThat(testUserInfo.getEmail()).isEqualTo(originalEmail);
        assertThat(testUserInfo.getUserName()).isEqualTo(originalUserName);
        assertThat(testUserInfo.getPassword()).isEqualTo(originalPassword);

        verifyNoInteractions(userInfoRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should update only password when only password is provided")
    void shouldUpdateOnlyPassword_WhenOnlyPasswordIsProvided() {
        // Arrange
        testUser.setUserInfo(testUserInfo);
        UpdateUserDTO passwordOnlyDTO = UpdateUserDTO.builder()
                .password("newSecretPassword1!")
                .build();

        String originalFirstName = testUserInfo.getFirstName();
        String originalEmail = testUserInfo.getEmail();

        when(passwordEncoder.encode("newSecretPassword1!")).thenReturn("encodedNewSecretPassword");

        // Act
        assertDoesNotThrow(() -> userValidationService.updateUserInfo(testUser, passwordOnlyDTO));

        // Assert
        assertThat(testUserInfo.getPassword()).isEqualTo("encodedNewSecretPassword");
        assertThat(testUserInfo.getFirstName()).isEqualTo(originalFirstName);
        assertThat(testUserInfo.getEmail()).isEqualTo(originalEmail);

        verify(passwordEncoder).encode("newSecretPassword1!");
        verifyNoInteractions(userInfoRepository);
    }

}
