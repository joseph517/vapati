package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.CreateVerificationRequestDTO;
import com.vaPaTi.vaPaTi.dtos.ProcessVerificationRequestDTO;
import com.vaPaTi.vaPaTi.dtos.VerificationStatusResponseDTO;
import com.vaPaTi.vaPaTi.entity.VerificationRequest;
import com.vaPaTi.vaPaTi.service.VerificationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/verifications")
@RequiredArgsConstructor
public class VerificationRequestController {

    private final VerificationRequestService service;

    @PostMapping("/request")
    @Operation(summary = "Create verification request")
    public ResponseEntity<Map<String, Object>> createVerificationRequest(@Valid @RequestBody CreateVerificationRequestDTO dto) {
        Long requestId = service.createVerificationRequest(dto);
        return ResponseEntity.ok(Map.of(
                "message", "Verification request created successfully",
                "requestId", requestId,
                "status", "PENDING"
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/process")
    @Operation(summary = "Process verification request")
    public ResponseEntity<Map<String, Object>> processVerificationRequest(@Valid @RequestBody ProcessVerificationRequestDTO dto) {
        VerificationRequest request = service.processVerificationRequest(dto);
        return ResponseEntity.ok(Map.of(
                "message", "Request processed successfully",
                "requestId", request.getId(),
                "status", request.getStatus(),
                "userVerified", request.getUser().isVerified()
        ));
    }

    @GetMapping("/status/{userId}")
    @Operation(summary = "Get verification status")
    public ResponseEntity<Object> getVerificationStatus(@PathVariable Long userId) {
        VerificationStatusResponseDTO dto = service.getVerificationStatus(userId);
        return ResponseEntity.ok(dto);
    }
}