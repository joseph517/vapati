package com.vaPaTi.vaPaTi.validation.verification;

import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.verification.VerificationRequest;
import com.vaPaTi.vaPaTi.entity.verification.VerificationStatus;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.repository.verification.VerificationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationRequestValidationService Tests")
public class VerificationRequestValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @InjectMocks
    private VerificationRequestValidationService verificationRequestValidationService;

    private User mockUser;
    private VerificationRequest mockVerificationRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .verified(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();

        mockVerificationRequest = new VerificationRequest();
        mockVerificationRequest.setId(1L);
        mockVerificationRequest.setUser(mockUser);
        mockVerificationRequest.setStatus(VerificationStatus.PENDING.name());
        mockVerificationRequest.setDniFront("dni_front.jpg");
        mockVerificationRequest.setDniBack("dni_back.jpg");
        mockVerificationRequest.setSelfieUser("selfie.jpg");
        mockVerificationRequest.setCreatedAt(LocalDateTime.now().minusHours(1));
    }

    @Nested
    @DisplayName("validateAndGetUser Tests")
    class ValidateAndGetUserTests {

        @Test
        @DisplayName("Should return user when user exists")
        void shouldReturnUserWhenUserExists() {
            // Given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            // When
            User result = verificationRequestValidationService.validateAndGetUser(userId);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(mockUser);
            assertThat(result.getId()).isEqualTo(userId);

            verify(userRepository, times(1)).findById(userId);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should throw MessageException when user does not exist")
        void shouldThrowMessageExceptionWhenUserDoesNotExist() {
            // Given
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> verificationRequestValidationService.validateAndGetUser(userId))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User not found.");

            verify(userRepository, times(1)).findById(userId);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should handle null userId gracefully")
        void shouldHandleNullUserIdGracefully() {
            // Given
            Long userId = null;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> verificationRequestValidationService.validateAndGetUser(userId))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User not found.");

            verify(userRepository, times(1)).findById(userId);
        }
    }

    @Nested
    @DisplayName("validateUserNotVerified Tests")
    class ValidateUserNotVerifiedTests {

        @Test
        @DisplayName("Should pass validation when user is not verified")
        void shouldPassValidationWhenUserIsNotVerified() {
            // Given
            mockUser.setVerified(false);

            // When & Then
            assertThatCode(() -> verificationRequestValidationService.validateUserNotVerified(mockUser))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw MessageException when user is already verified")
        void shouldThrowMessageExceptionWhenUserIsAlreadyVerified() {
            // Given
            mockUser.setVerified(true);

            // When & Then
            assertThatThrownBy(() -> verificationRequestValidationService.validateUserNotVerified(mockUser))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User is already verified.");
        }

        @Test
        @DisplayName("Should handle user with default verified state (false)")
        void shouldHandleUserWithDefaultVerifiedState() {
            // Given
            User userWithDefaultState = User.builder()
                    .id(2L)
                    .build(); // verified defaults to false

            // When & Then
            assertThatCode(() -> verificationRequestValidationService.validateUserNotVerified(userWithDefaultState))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("findVerificationRequestById Tests")
    class FindVerificationRequestByIdTests {

        @Test
        @DisplayName("Should return verification request when request exists")
        void shouldReturnVerificationRequestWhenRequestExists() {
            // Given
            Long requestId = 1L;
            when(verificationRequestRepository.findById(requestId))
                    .thenReturn(Optional.of(mockVerificationRequest));

            // When
            VerificationRequest result = verificationRequestValidationService.findVerificationRequestById(requestId);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(mockVerificationRequest);
            assertThat(result.getId()).isEqualTo(requestId);
            assertThat(result.getStatus()).isEqualTo(VerificationStatus.PENDING.name());

            verify(verificationRequestRepository, times(1)).findById(requestId);
            verifyNoMoreInteractions(verificationRequestRepository);
        }

        @Test
        @DisplayName("Should throw MessageException when verification request does not exist")
        void shouldThrowMessageExceptionWhenVerificationRequestDoesNotExist() {
            // Given
            Long requestId = 999L;
            when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> verificationRequestValidationService.findVerificationRequestById(requestId))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Verification request not found.");

            verify(verificationRequestRepository, times(1)).findById(requestId);
            verifyNoMoreInteractions(verificationRequestRepository);
        }

        @Test
        @DisplayName("Should handle null requestId gracefully")
        void shouldHandleNullRequestIdGracefully() {
            // Given
            Long requestId = null;
            when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> verificationRequestValidationService.findVerificationRequestById(requestId))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Verification request not found.");

            verify(verificationRequestRepository, times(1)).findById(requestId);
        }
    }

    @Nested
    @DisplayName("isPendingRequest Tests")
    class IsPendingRequestTests {

        @Test
        @DisplayName("Should return true when request status is PENDING")
        void shouldReturnTrueWhenRequestStatusIsPending() {
            // Given
            mockVerificationRequest.setStatus(VerificationStatus.PENDING.name());

            // When
            boolean result = verificationRequestValidationService.isPendingRequest(mockVerificationRequest);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when request status is APPROVED")
        void shouldReturnFalseWhenRequestStatusIsApproved() {
            // Given
            mockVerificationRequest.setStatus(VerificationStatus.APPROVED.name());

            // When
            boolean result = verificationRequestValidationService.isPendingRequest(mockVerificationRequest);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when request status is REJECTED")
        void shouldReturnFalseWhenRequestStatusIsRejected() {
            // Given
            mockVerificationRequest.setStatus(VerificationStatus.REJECTED.name());

            // When
            boolean result = verificationRequestValidationService.isPendingRequest(mockVerificationRequest);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle lowercase status string")
        void shouldHandleLowercaseStatusString() {
            // Given
            mockVerificationRequest.setStatus("pending");

            // When & Then
            assertThatThrownBy(() -> verificationRequestValidationService.isPendingRequest(mockVerificationRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle invalid status string")
        void shouldHandleInvalidStatusString() {
            // Given
            mockVerificationRequest.setStatus("INVALID_STATUS");

            // When & Then
            assertThatThrownBy(() -> verificationRequestValidationService.isPendingRequest(mockVerificationRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isRejectedRequest Tests")
    class IsRejectedRequestTests {

        @Test
        @DisplayName("Should return true when request status is REJECTED")
        void shouldReturnTrueWhenRequestStatusIsRejected() {
            // Given
            mockVerificationRequest.setStatus(VerificationStatus.REJECTED.name());

            // When
            boolean result = verificationRequestValidationService.isRejectedRequest(mockVerificationRequest);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when request status is PENDING")
        void shouldReturnFalseWhenRequestStatusIsPending() {
            // Given
            mockVerificationRequest.setStatus(VerificationStatus.PENDING.name());

            // When
            boolean result = verificationRequestValidationService.isRejectedRequest(mockVerificationRequest);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when request status is APPROVED")
        void shouldReturnFalseWhenRequestStatusIsApproved() {
            // Given
            mockVerificationRequest.setStatus(VerificationStatus.APPROVED.name());

            // When
            boolean result = verificationRequestValidationService.isRejectedRequest(mockVerificationRequest);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle invalid status string")
        void shouldHandleInvalidStatusString() {
            // Given
            mockVerificationRequest.setStatus("UNKNOWN_STATUS");

            // When & Then
            assertThatThrownBy(() -> verificationRequestValidationService.isRejectedRequest(mockVerificationRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isApproved Tests")
    class IsApprovedTests {

        @Test
        @DisplayName("Should return true when status is APPROVED")
        void shouldReturnTrueWhenStatusIsApproved() {
            // Given
            VerificationStatus status = VerificationStatus.APPROVED;

            // When
            boolean result = verificationRequestValidationService.isApproved(status);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when status is PENDING")
        void shouldReturnFalseWhenStatusIsPending() {
            // Given
            VerificationStatus status = VerificationStatus.PENDING;

            // When
            boolean result = verificationRequestValidationService.isApproved(status);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when status is REJECTED")
        void shouldReturnFalseWhenStatusIsRejected() {
            // Given
            VerificationStatus status = VerificationStatus.REJECTED;

            // When
            boolean result = verificationRequestValidationService.isApproved(status);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle null status")
        void shouldHandleNullStatus() {
            // Given
            VerificationStatus status = null;

            // When
            boolean result = verificationRequestValidationService.isApproved(status);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("approveUserVerification Tests")
    class ApproveUserVerificationTests {

        @Test
        @DisplayName("Should approve user verification and save user")
        void shouldApproveUserVerificationAndSaveUser() {
            // Given
            LocalDateTime beforeUpdate = LocalDateTime.now();
            mockUser.setVerified(false);
            mockUser.setUpdatedAt(beforeUpdate.minusMinutes(5));

            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            // When
            verificationRequestValidationService.approveUserVerification(mockUser);

            // Then
            assertThat(mockUser.isVerified()).isTrue();
            assertThat(mockUser.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);

            verify(userRepository, times(1)).save(mockUser);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should update timestamp when approving already verified user")
        void shouldUpdateTimestampWhenApprovingAlreadyVerifiedUser() {
            // Given
            LocalDateTime beforeUpdate = LocalDateTime.now().minusMinutes(5);
            mockUser.setVerified(true);
            mockUser.setUpdatedAt(beforeUpdate.minusMinutes(5));

            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            // When
            verificationRequestValidationService.approveUserVerification(mockUser);

            // Then
            assertThat(mockUser.isVerified()).isTrue();
            assertThat(mockUser.getUpdatedAt()).isAfter(beforeUpdate);

            verify(userRepository, times(1)).save(mockUser);
        }

        @Test
        @DisplayName("Should maintain user state integrity during approval")
        void shouldMaintainUserStateIntegrityDuringApproval() {
            // Given
            Long originalId = mockUser.getId();
            LocalDateTime originalCreatedAt = mockUser.getCreatedAt();

            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            // When
            verificationRequestValidationService.approveUserVerification(mockUser);

            // Then
            assertThat(mockUser.getId()).isEqualTo(originalId);
            assertThat(mockUser.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(mockUser.isVerified()).isTrue();

            verify(userRepository, times(1)).save(mockUser);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should execute complete user verification flow in correct order")
        void shouldExecuteCompleteUserVerificationFlowInCorrectOrder() {
            // Given
            Long userId = 1L;
            mockUser.setVerified(false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            InOrder inOrder = inOrder(userRepository);

            // When
            User foundUser = verificationRequestValidationService.validateAndGetUser(userId);
            verificationRequestValidationService.validateUserNotVerified(foundUser);
            verificationRequestValidationService.approveUserVerification(foundUser);

            // Then
            assertThat(foundUser.isVerified()).isTrue();

            inOrder.verify(userRepository).findById(userId);
            inOrder.verify(userRepository).save(mockUser);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should handle verification request status checks consistently")
        void shouldHandleVerificationRequestStatusChecksConsistently() {
            // Given
            VerificationRequest pendingRequest = new VerificationRequest();
            pendingRequest.setStatus(VerificationStatus.PENDING.name());

            VerificationRequest approvedRequest = new VerificationRequest();
            approvedRequest.setStatus(VerificationStatus.APPROVED.name());

            VerificationRequest rejectedRequest = new VerificationRequest();
            rejectedRequest.setStatus(VerificationStatus.REJECTED.name());

            // When & Then
            assertThat(verificationRequestValidationService.isPendingRequest(pendingRequest)).isTrue();
            assertThat(verificationRequestValidationService.isPendingRequest(approvedRequest)).isFalse();
            assertThat(verificationRequestValidationService.isPendingRequest(rejectedRequest)).isFalse();

            assertThat(verificationRequestValidationService.isRejectedRequest(rejectedRequest)).isTrue();
            assertThat(verificationRequestValidationService.isRejectedRequest(pendingRequest)).isFalse();
            assertThat(verificationRequestValidationService.isRejectedRequest(approvedRequest)).isFalse();

            assertThat(verificationRequestValidationService.isApproved(VerificationStatus.APPROVED)).isTrue();
            assertThat(verificationRequestValidationService.isApproved(VerificationStatus.PENDING)).isFalse();
            assertThat(verificationRequestValidationService.isApproved(VerificationStatus.REJECTED)).isFalse();
        }
    }

}
