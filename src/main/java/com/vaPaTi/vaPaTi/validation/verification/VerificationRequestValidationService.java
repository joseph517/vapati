package com.vaPaTi.vaPaTi.validation.verification;

import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.verification.VerificationRequest;
import com.vaPaTi.vaPaTi.entity.verification.VerificationStatus;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.repository.verification.VerificationRequestRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VerificationRequestValidationService {

    private final UserRepository userRepository;
    private final VerificationRequestRepository verificationRequestRepository;

    // Constructor
    public VerificationRequestValidationService(
            UserRepository userRepository,
            VerificationRequestRepository verificationRequestRepository
    ) {
        this.userRepository = userRepository;
        this.verificationRequestRepository = verificationRequestRepository;
    }


    public User validateAndGetUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    public void validateUserNotVerified(@NotNull User user) {
        if (user.isVerified()) {
            throw new ConflictException("User is already verified.");
        }
    }

    public VerificationRequest findVerificationRequestById(Long requestId) {
        return verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found."));
    }

    public boolean isPendingRequest(@NotNull VerificationRequest request) {
        return VerificationStatus.valueOf(request.getStatus()) == VerificationStatus.PENDING;
    }

    public boolean isRejectedRequest(@NotNull VerificationRequest request) {
        return VerificationStatus.valueOf(request.getStatus()) == VerificationStatus.REJECTED;
    }

    public boolean isApproved(VerificationStatus status) {
        return status == VerificationStatus.APPROVED;
    }

    public void approveUserVerification(@NotNull User user) {
        user.setVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
