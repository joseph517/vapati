package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CreateVerificationRequestDTO;
import com.vaPaTi.vaPaTi.dtos.ProcessVerificationRequestDTO;
import com.vaPaTi.vaPaTi.dtos.VerificationStatusResponseDTO;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.VerificationRequest;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.VerificationRequestMapper;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.repository.VerificationRequestRepository;
import com.vaPaTi.vaPaTi.validation.VerificationStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class VerificationRequestService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final UserRepository userRepository;
    private final VerificationRequestMapper verificationRequestMapper;

    public VerificationRequestService(
            VerificationRequestRepository verificationRequestRepository,
            UserRepository userRepository,
            VerificationRequestMapper verificationRequestMapper
    ) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.userRepository = userRepository;
        this.verificationRequestMapper = verificationRequestMapper;
    }

    public Long createVerificationRequest(@NotNull CreateVerificationRequestDTO dto) {
        User user = validateAndGetUser(dto.getUserId());
        validateUserNotVerified(user);

        Optional<VerificationRequest> existingRequest = verificationRequestRepository.findByUserId(dto.getUserId());

        if (existingRequest.isPresent()) {
            VerificationRequest request = existingRequest.get();

            if (isPendingRequest(request)) {
                throw new MessageException("There is already a pending request for this user.");
            }

            if (isRejectedRequest(request)) {
                verificationRequestMapper.updateFromDto(request, dto);
                VerificationRequest updatedRequest = verificationRequestRepository.save(request);
                return updatedRequest.getId();
            }
        }

        VerificationRequest newRequest = verificationRequestMapper.toEntity(dto, user);
        VerificationRequest savedRequest = verificationRequestRepository.save(newRequest);
        return savedRequest.getId();
    }

    public VerificationRequest processVerificationRequest(@NotNull ProcessVerificationRequestDTO dto) {
        VerificationRequest request = findVerificationRequestById(dto.getRequestId());
        request.setStatus(dto.getStatus().name());
        request.setUpdatedAt(LocalDateTime.now());

        if (isApproved(dto.getStatus())) {
            approveUserVerification(request.getUser());
        }

        return verificationRequestRepository.save(request);
    }

    public VerificationStatusResponseDTO getVerificationStatus(Long userId) {
        VerificationRequest request = verificationRequestRepository.findByUserId(userId)
                .orElseThrow(() -> new MessageException("No verification request found."));
        return verificationRequestMapper.toStatusDTO(request);
    }


    private User validateAndGetUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new MessageException("User not found."));
    }

    private void validateUserNotVerified(@NotNull User user) {
        if (user.isVerified()) {
            throw new MessageException("User is already verified.");
        }
    }

    private VerificationRequest findVerificationRequestById(Long requestId) {
        return verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new MessageException("Verification request not found."));
    }

    private boolean isPendingRequest(@NotNull VerificationRequest request) {
        return VerificationStatus.valueOf(request.getStatus()) == VerificationStatus.PENDING;
    }

    private boolean isRejectedRequest(@NotNull VerificationRequest request) {
        return VerificationStatus.valueOf(request.getStatus()) == VerificationStatus.REJECTED;
    }

    private boolean isApproved(VerificationStatus status) {
        return status == VerificationStatus.APPROVED;
    }

    private void approveUserVerification(@NotNull User user) {
        user.setVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
