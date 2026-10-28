package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserValidationService - User Timestamp Tests")
class UserValidationServiceTimestampTest {

    @InjectMocks
    private UserValidationService timestampUpdateService;

    private User testUser;
    private LocalDateTime fixedDateTime;

    @BeforeEach
    void setUp() {
        fixedDateTime = LocalDateTime.of(2024, 1, 15, 12, 30, 45);

        testUser = User.builder()
                .id(1L)
                .verified(false)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 10, 15, 30, 0))
                .build();
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
    @DisplayName("Should not affect other User properties when updating timestamp")
    void updateTimestamp_ShouldOnlyUpdateTimestamp() {
        // Given
        Long originalId = testUser.getId();
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
            assertThat(testUser.isVerified()).isEqualTo(originalVerified);
            assertThat(testUser.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(testUser.getDeletedAt()).isEqualTo(originalDeletedAt);
        }
    }
}
