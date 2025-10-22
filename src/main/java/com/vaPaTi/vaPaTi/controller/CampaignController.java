package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

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


    @GetMapping("/list")
    @Operation(summary = "Get all campaigns")
    public List<CampaignResponseDTO> getAllCampaigns() {
        return campaignService.getAllCampaigns();
    }

    @GetMapping("/my-campaigns")
    @Operation(summary = "Get all campaigns of authenticated user")
    public ResponseEntity<Map<String, Object>> getMyCampaigns() {
        try {
            List<CampaignResponseDTO> campaigns = campaignService.getCampaignsByAuthenticatedUser();

            return ResponseEntity.ok(Map.of(
                    "message", "Campaigns retrieved successfully",
                    "campaigns", campaigns,
                    "total", campaigns.size()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<CampaignResponseDTO> updateCampaign(
            @PathVariable Long campaignId,
            @RequestBody @Valid UpdateCampaignRequestDTO dto) {
        CampaignResponseDTO responseDTO = campaignService.updateCampaign(campaignId, dto);
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Map<String, Object>> deleteCampaign(@PathVariable Long campaignId) {
        try {
            campaignService.deleteCampaign(campaignId);
            return ResponseEntity.ok(Map.of(
                    "message", "Campaign deleted successfully",
                    "campaignId", campaignId
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/{campaignId}/close")
    @Operation(summary = "Close campaign (deactivate goal)")
    public ResponseEntity<Map<String, Object>> closeCampaign(@PathVariable Long campaignId) {
        try {
            CampaignResponseDTO responseDTO = campaignService.closeCampaign(campaignId);
            return ResponseEntity.ok(Map.of(
                    "message", "Campaign closed successfully",
                    "campaign", responseDTO
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

}

