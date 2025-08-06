package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping("/list")
    public List<CampaignResponseDTO> getAllCampaigns() {
        return campaignService.getAllCampaigns();
    }

    @PostMapping("/create")
    @Operation(summary = "Create campaign")
    public ResponseEntity<Map<String, Object>> createCampaign(@Valid @RequestBody CreateCampaignRequestDTO dto) {
        try {
            CampaignResponseDTO responseDTO = campaignService.createCampaign(dto);

            return ResponseEntity.ok(Map.of(
                    "message", "Campaign created successfully",
                    "campaignId", responseDTO.getId(),
                    "status", "CREATED"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

}

