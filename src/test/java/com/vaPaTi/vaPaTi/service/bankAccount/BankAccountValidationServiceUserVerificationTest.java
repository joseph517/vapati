package com.vaPaTi.vaPaTi.service.bankAccount;

import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.BankAccountRepository;
import com.vaPaTi.vaPaTi.validation.BankAccountValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountValidationService - User Verification Tests")
class BankAccountValidationServiceUserVerificationTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    private BankAccountValidationService validationService;
    private static final Long VALID_USER_ID = 1L;
    private static final Long INVALID_USER_ID = 999L;
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";
    private static final String USER_NOT_VERIFIED_MESSAGE = "User is not verified";

    @BeforeEach
    void setUp() {
        validationService = new BankAccountValidationService(bankAccountRepository);
    }

    @Nested
    @DisplayName("verifyUserIsVerified method tests")
    class VerifyUserIsVerifiedTests {

        @Test
        @DisplayName("should pass verification when user exists and is verified")
        void shouldPassVerification_WhenUserExistsAndIsVerified() {
            // Given
            when(bankAccountRepository.isUserVerified(VALID_USER_ID))
                    .thenReturn(Optional.of(true));

            // When & Then
            assertDoesNotThrow(() -> validationService.verifyUserIsVerified(VALID_USER_ID));

            // Verify interaction
            verify(bankAccountRepository, times(1)).isUserVerified(VALID_USER_ID);
            verifyNoMoreInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should throw MessageException with USER_NOT_FOUND when user does not exist")
        void shouldThrowMessageException_WhenUserDoesNotExist() {
            // Given
            when(bankAccountRepository.isUserVerified(INVALID_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.verifyUserIsVerified(INVALID_USER_ID)
            );

            assertEquals(USER_NOT_FOUND_MESSAGE, exception.getMessage());

            // Verify interaction
            verify(bankAccountRepository, times(1)).isUserVerified(INVALID_USER_ID);
            verifyNoMoreInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should throw MessageException with USER_NOT_VERIFIED when user exists but is not verified")
        void shouldThrowMessageException_WhenUserExistsButIsNotVerified() {
            // Given
            when(bankAccountRepository.isUserVerified(VALID_USER_ID))
                    .thenReturn(Optional.of(false));

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.verifyUserIsVerified(VALID_USER_ID)
            );

            assertEquals(USER_NOT_VERIFIED_MESSAGE, exception.getMessage());

            // Verify interaction
            verify(bankAccountRepository, times(1)).isUserVerified(VALID_USER_ID);
            verifyNoMoreInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should handle null userId by calling repository with null")
        void shouldHandleNullUserId_ByCallingRepositoryWithNull() {
            // Given
            when(bankAccountRepository.isUserVerified(null))
                    .thenReturn(Optional.empty());

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.verifyUserIsVerified(null)
            );

            assertEquals(USER_NOT_FOUND_MESSAGE, exception.getMessage());

            // Verify interaction
            verify(bankAccountRepository, times(1)).isUserVerified(null);
            verifyNoMoreInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should handle zero userId correctly")
        void shouldHandleZeroUserId_Correctly() {
            // Given
            Long zeroUserId = 0L;
            when(bankAccountRepository.isUserVerified(zeroUserId))
                    .thenReturn(Optional.of(true));

            // When & Then
            assertDoesNotThrow(() -> validationService.verifyUserIsVerified(zeroUserId));

            // Verify interaction
            verify(bankAccountRepository, times(1)).isUserVerified(zeroUserId);
            verifyNoMoreInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should handle negative userId correctly")
        void shouldHandleNegativeUserId_Correctly() {
            // Given
            Long negativeUserId = -1L;
            when(bankAccountRepository.isUserVerified(negativeUserId))
                    .thenReturn(Optional.of(false));

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.verifyUserIsVerified(negativeUserId)
            );

            assertEquals(USER_NOT_VERIFIED_MESSAGE, exception.getMessage());

            // Verify interaction
            verify(bankAccountRepository, times(1)).isUserVerified(negativeUserId);
            verifyNoMoreInteractions(bankAccountRepository);
        }

        @Test
        @DisplayName("should handle very large userId correctly")
        void shouldHandleVeryLargeUserId_Correctly() {
            // Given
            Long largeUserId = Long.MAX_VALUE;
            when(bankAccountRepository.isUserVerified(largeUserId))
                    .thenReturn(Optional.of(true));

            // When & Then
            assertDoesNotThrow(() -> validationService.verifyUserIsVerified(largeUserId));

            // Verify interaction
            verify(bankAccountRepository, times(1)).isUserVerified(largeUserId);
            verifyNoMoreInteractions(bankAccountRepository);
        }
    }

    @Nested
    @DisplayName("Method execution order and behavior tests")
    class ExecutionOrderAndBehaviorTests {

        @Test
        @DisplayName("should call repository only once for successful verification")
        void shouldCallRepositoryOnlyOnce_ForSuccessfulVerification() {
            // Given
            when(bankAccountRepository.isUserVerified(VALID_USER_ID))
                    .thenReturn(Optional.of(true));

            // When
            validationService.verifyUserIsVerified(VALID_USER_ID);

            // Then - Verify exact number of interactions
            verify(bankAccountRepository, times(1)).isUserVerified(VALID_USER_ID);
            verify(bankAccountRepository, never()).isUserVerified(argThat(id -> !VALID_USER_ID.equals(id)));
        }

        @Test
        @DisplayName("should call repository only once before throwing USER_NOT_FOUND exception")
        void shouldCallRepositoryOnlyOnce_BeforeThrowingUserNotFoundException() {
            // Given
            when(bankAccountRepository.isUserVerified(INVALID_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThrows(MessageException.class,
                    () -> validationService.verifyUserIsVerified(INVALID_USER_ID));

            // Verify execution order
            InOrder inOrder = inOrder(bankAccountRepository);
            inOrder.verify(bankAccountRepository).isUserVerified(INVALID_USER_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("should call repository only once before throwing USER_NOT_VERIFIED exception")
        void shouldCallRepositoryOnlyOnce_BeforeThrowingUserNotVerifiedException() {
            // Given
            when(bankAccountRepository.isUserVerified(VALID_USER_ID))
                    .thenReturn(Optional.of(false));

            // When & Then
            assertThrows(MessageException.class,
                    () -> validationService.verifyUserIsVerified(VALID_USER_ID));

            // Verify execution order
            InOrder inOrder = inOrder(bankAccountRepository);
            inOrder.verify(bankAccountRepository).isUserVerified(VALID_USER_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND before checking verification status when user does not exist")
        void shouldThrowUserNotFound_BeforeCheckingVerificationStatus_WhenUserDoesNotExist() {
            // Given
            when(bankAccountRepository.isUserVerified(INVALID_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            MessageException exception = assertThrows(
                    MessageException.class,
                    () -> validationService.verifyUserIsVerified(INVALID_USER_ID)
            );

            // Verify the correct exception is thrown (USER_NOT_FOUND, not USER_NOT_VERIFIED)
            assertEquals(USER_NOT_FOUND_MESSAGE, exception.getMessage());
            assertNotEquals(USER_NOT_VERIFIED_MESSAGE, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Repository interaction and exception handling tests")
    class RepositoryInteractionTests {

        @Test
        @DisplayName("should propagate repository runtime exception when repository throws unexpected exception")
        void shouldPropagateRepositoryException_WhenRepositoryThrowsUnexpectedException() {
            // Given
            RuntimeException repositoryException = new RuntimeException("Database connection failed");
            when(bankAccountRepository.isUserVerified(VALID_USER_ID))
                    .thenThrow(repositoryException);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> validationService.verifyUserIsVerified(VALID_USER_ID)
            );

            assertEquals("Database connection failed", exception.getMessage());
            assertSame(repositoryException, exception);

            // Verify interaction
            verify(bankAccountRepository, times(1)).isUserVerified(VALID_USER_ID);
        }

        @Test
        @DisplayName("should handle repository returning null Optional correctly")
        void shouldHandleRepositoryReturningNull_Correctly() {
            // Given - This scenario shouldn't happen in well-designed code, but testing defensive programming
            when(bankAccountRepository.isUserVerified(VALID_USER_ID))
                    .thenReturn(null);

            // When & Then - Should throw NullPointerException due to calling orElseThrow on null
            assertThrows(
                    NullPointerException.class,
                    () -> validationService.verifyUserIsVerified(VALID_USER_ID)
            );

            verify(bankAccountRepository, times(1)).isUserVerified(VALID_USER_ID);
        }

        @Test
        @DisplayName("should verify exact method signature is called on repository")
        void shouldVerifyExactMethodSignature_IsCalledOnRepository() {
            // Given
            Long specificUserId = 42L;
            when(bankAccountRepository.isUserVerified(specificUserId))
                    .thenReturn(Optional.of(true));

            // When
            validationService.verifyUserIsVerified(specificUserId);

            // Then - Verify exact method call with exact parameter
            verify(bankAccountRepository).isUserVerified(eq(specificUserId));
            verify(bankAccountRepository, never()).isUserVerified(argThat(id -> !specificUserId.equals(id)));
        }
    }

    @Nested
    @DisplayName("Multiple invocation and state consistency tests")
    class MultipleInvocationTests {

        @Test
        @DisplayName("should handle multiple successful verifications for same user")
        void shouldHandleMultipleSuccessfulVerifications_ForSameUser() {
            // Given
            when(bankAccountRepository.isUserVerified(VALID_USER_ID))
                    .thenReturn(Optional.of(true));

            // When
            assertDoesNotThrow(() -> validationService.verifyUserIsVerified(VALID_USER_ID));
            assertDoesNotThrow(() -> validationService.verifyUserIsVerified(VALID_USER_ID));
            assertDoesNotThrow(() -> validationService.verifyUserIsVerified(VALID_USER_ID));

            // Then
            verify(bankAccountRepository, times(3)).isUserVerified(VALID_USER_ID);
        }

        @Test
        @DisplayName("should handle multiple failed verifications consistently")
        void shouldHandleMultipleFailedVerifications_Consistently() {
            // Given
            when(bankAccountRepository.isUserVerified(INVALID_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            for (int i = 0; i < 3; i++) {
                MessageException exception = assertThrows(
                        MessageException.class,
                        () -> validationService.verifyUserIsVerified(INVALID_USER_ID)
                );
                assertEquals(USER_NOT_FOUND_MESSAGE, exception.getMessage());
            }

            verify(bankAccountRepository, times(3)).isUserVerified(INVALID_USER_ID);
        }

        @Test
        @DisplayName("should handle different users with different verification statuses in sequence")
        void shouldHandleDifferentUsers_WithDifferentVerificationStatuses_InSequence() {
            // Given
            Long verifiedUserId = 1L;
            Long unverifiedUserId = 2L;
            Long nonExistentUserId = 3L;

            when(bankAccountRepository.isUserVerified(verifiedUserId))
                    .thenReturn(Optional.of(true));
            when(bankAccountRepository.isUserVerified(unverifiedUserId))
                    .thenReturn(Optional.of(false));
            when(bankAccountRepository.isUserVerified(nonExistentUserId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertDoesNotThrow(() -> validationService.verifyUserIsVerified(verifiedUserId));

            MessageException unverifiedException = assertThrows(
                    MessageException.class,
                    () -> validationService.verifyUserIsVerified(unverifiedUserId)
            );
            assertEquals(USER_NOT_VERIFIED_MESSAGE, unverifiedException.getMessage());

            MessageException notFoundException = assertThrows(
                    MessageException.class,
                    () -> validationService.verifyUserIsVerified(nonExistentUserId)
            );
            assertEquals(USER_NOT_FOUND_MESSAGE, notFoundException.getMessage());

            // Verify all interactions
            InOrder inOrder = inOrder(bankAccountRepository);
            inOrder.verify(bankAccountRepository).isUserVerified(verifiedUserId);
            inOrder.verify(bankAccountRepository).isUserVerified(unverifiedUserId);
            inOrder.verify(bankAccountRepository).isUserVerified(nonExistentUserId);
            inOrder.verifyNoMoreInteractions();
        }
    }

}
