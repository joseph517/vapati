package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.CreateVerificationRequestDTO;
import com.vaPaTi.vaPaTi.dtos.VerificationStatusResponseDTO;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.VerificationRequest;
import com.vaPaTi.vaPaTi.validation.VerificationStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VerificationRequestMapper {

    public VerificationRequest toEntity(@NotNull CreateVerificationRequestDTO dto, User user) {
        VerificationRequest request = new VerificationRequest();
        request.setUser(user);
        request.setDniFront(dto.getDniFront());
        request.setDniBack(dto.getDniBack());
        request.setSelfieUser(dto.getSelfieUser());
        request.setStatus(VerificationStatus.PENDING.name());
        LocalDateTime now = LocalDateTime.now();
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        return request;
    }

    public void updateFromDto(@NotNull VerificationRequest request, @NotNull CreateVerificationRequestDTO dto) {
        request.setDniFront(dto.getDniFront());
        request.setDniBack(dto.getDniBack());
        request.setSelfieUser(dto.getSelfieUser());
        request.setStatus(VerificationStatus.PENDING.name());
        request.setUpdatedAt(LocalDateTime.now());
    }

    public VerificationStatusResponseDTO toStatusDTO(VerificationRequest request) {
        VerificationStatusResponseDTO dto = new VerificationStatusResponseDTO();
        dto.setVerified(request.getUser().isVerified());
        dto.setRequestStatus(VerificationStatus.valueOf(request.getStatus()));
        dto.setCreatedAt(request.getCreatedAt());
        return dto;
    }
}