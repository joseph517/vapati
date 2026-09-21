package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CampaignStatusHistoryResponseDTO;
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
        CampaignResponseDTO responseDTO = campaignService.createCampaign(dto);

        return ResponseEntity.ok(Map.of(
                "message", "Campaign created successfully",
                "campaignId", responseDTO.getId(),
                "status", "CREATED"
        ));
    }


    @GetMapping("/list")
    @Operation(summary = "Get all campaigns")
    public List<CampaignResponseDTO> getAllCampaigns(@RequestParam(required = false) Long categoryId,
                                                       @RequestParam(required = false) String status) {
        if (status != null) {
            return campaignService.getCampaignsByStatus(status);
        }
        if (categoryId != null) {
            return campaignService.getCampaignsByCategoryId(categoryId);
        }
        return campaignService.getAllCampaigns();
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Get campaign by id")
    public ResponseEntity<CampaignResponseDTO> getCampaignById(@PathVariable Long campaignId) {
        return ResponseEntity.ok(campaignService.getCampaignById(campaignId));
    }

    @GetMapping("/my-campaigns")
    @Operation(summary = "Get all campaigns of authenticated user")
    public ResponseEntity<Map<String, Object>> getMyCampaigns() {
        List<CampaignResponseDTO> campaigns = campaignService.getCampaignsByAuthenticatedUser();

        return ResponseEntity.ok(Map.of(
                "message", "Campaigns retrieved successfully",
                "campaigns", campaigns,
                "total", campaigns.size()
        ));
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
        campaignService.deleteCampaign(campaignId);
        return ResponseEntity.ok(Map.of(
                "message", "Campaign deleted successfully",
                "campaignId", campaignId
        ));
    }

    @PutMapping("/{campaignId}/close")
    @Operation(summary = "Close campaign (deactivate goal)")
    public ResponseEntity<Map<String, Object>> closeCampaign(@PathVariable Long campaignId) {
        CampaignResponseDTO responseDTO = campaignService.closeCampaign(campaignId);
        return ResponseEntity.ok(Map.of(
                "message", "Campaign closed successfully",
                "campaign", responseDTO
        ));
    }

    @PutMapping("/{campaignId}/activate")
    @Operation(summary = "Activate a previously closed campaign")
    public ResponseEntity<Map<String, Object>> activateCampaign(@PathVariable Long campaignId) {
        CampaignResponseDTO responseDTO = campaignService.activateCampaign(campaignId);
        return ResponseEntity.ok(Map.of(
                "message", "Campaign activated successfully",
                "campaign", responseDTO
        ));
    }

    @GetMapping("/{campaignId}/status-history")
    @Operation(summary = "Get the status transition history of a campaign")
    public ResponseEntity<List<CampaignStatusHistoryResponseDTO>> getCampaignStatusHistory(@PathVariable Long campaignId) {
        return ResponseEntity.ok(campaignService.getCampaignStatusHistory(campaignId));
    }

}

