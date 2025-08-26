package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;

import com.vaPaTi.vaPaTi.validation.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("Timestamp Update Service Tests")
class UserValidationServiceUserUpdateTest {

    @InjectMocks
    private UserValidationService timestampUpdateService;

    private User testUser;
    private UserInfo testUserInfo;
    private LocalDateTime fixedDateTime;

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
    @DisplayName("Should throw NullPointerException when UserInfo is null")
    void updateUserInfoTimestamp_WithNullUserInfo_ShouldThrowNullPointerException() {
        // Given
        UserInfo nullUserInfo = null;

        // When & Then
        assertThatThrownBy(() -> timestampUpdateService.updateUserInfoTimestamp(nullUserInfo))
                .isInstanceOf(NullPointerException.class);

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
                .isInstanceOf(NullPointerException.class);

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
}
