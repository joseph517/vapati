package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CreateVerificationRequestDTO;
import com.vaPaTi.vaPaTi.dtos.ProcessVerificationRequestDTO;
import com.vaPaTi.vaPaTi.dtos.VerificationStatusResponseDTO;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.VerificationRequest;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.VerificationRequestMapper;
import com.vaPaTi.vaPaTi.repository.VerificationRequestRepository;
import com.vaPaTi.vaPaTi.validation.VerificationRequestValitation;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class VerificationRequestService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationRequestMapper verificationRequestMapper;
    private final VerificationRequestValitation verificationRequestValitation;

    public VerificationRequestService(
            VerificationRequestRepository verificationRequestRepository,
            VerificationRequestMapper verificationRequestMapper,
            VerificationRequestValitation verificationRequestValitation
    ) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.verificationRequestMapper = verificationRequestMapper;
        this.verificationRequestValitation = verificationRequestValitation;
    }

    public Long createVerificationRequest(@NotNull CreateVerificationRequestDTO dto) {
        User user = verificationRequestValitation.validateAndGetUser(dto.getUserId());
        verificationRequestValitation.validateUserNotVerified(user);

        Optional<VerificationRequest> existingRequest = verificationRequestRepository.findByUserId(dto.getUserId());

        if (existingRequest.isPresent()) {
            VerificationRequest request = existingRequest.get();

            if (verificationRequestValitation.isPendingRequest(request)) {
                throw new MessageException("There is already a pending request for this user.");
            }

            if (verificationRequestValitation.isRejectedRequest(request)) {
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
        VerificationRequest request = verificationRequestValitation.findVerificationRequestById(dto.getRequestId());
        request.setStatus(dto.getStatus().name());
        request.setUpdatedAt(LocalDateTime.now());

        if (verificationRequestValitation.isApproved(dto.getStatus())) {
            verificationRequestValitation.approveUserVerification(request.getUser());
        }

        return verificationRequestRepository.save(request);
    }

    public VerificationStatusResponseDTO getVerificationStatus(Long userId) {
        VerificationRequest request = verificationRequestRepository.findByUserId(userId)
                .orElseThrow(() -> new MessageException("No verification request found."));
        return verificationRequestMapper.toStatusDTO(request);
    }

}
