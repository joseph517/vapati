package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.CampaignStatisticsDTO;
import com.vaPaTi.vaPaTi.dtos.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.DonationResponseDTO;
import com.vaPaTi.vaPaTi.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Donation management endpoints")
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    @Operation(summary = "Create a donation to a campaign")
    public ResponseEntity<Map<String, Object>> createDonation(@Valid @RequestBody CreateDonationDTO dto) {
        try {
            DonationResponseDTO responseDTO = donationService.createDonation(dto);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Donation completed successfully",
                    "donation", responseDTO,
                    "status", "COMPLETED"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/my-donations")
    @Operation(summary = "Get all donations made by authenticated user")
    public ResponseEntity<Map<String, Object>> getMyDonations() {
        try {
            List<DonationResponseDTO> donations = donationService.getDonationsByAuthenticatedUser();

            return ResponseEntity.ok(Map.of(
                    "message", "Donations retrieved successfully",
                    "donations", donations,
                    "total", donations.size()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "Get all donations for a specific campaign")
    public ResponseEntity<Map<String, Object>> getDonationsByCampaign(@PathVariable Long campaignId) {
        try {
            List<DonationResponseDTO> donations = donationService.getDonationsByCampaign(campaignId);

            return ResponseEntity.ok(Map.of(
                    "message", "Campaign donations retrieved successfully",
                    "donations", donations,
                    "total", donations.size()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/campaign/{campaignId}/statistics")
    @Operation(summary = "Get donation statistics for a campaign")
    public ResponseEntity<Map<String, Object>> getCampaignStatistics(@PathVariable Long campaignId) {
        try {
            CampaignStatisticsDTO statistics = donationService.getCampaignStatistics(campaignId);

            return ResponseEntity.ok(Map.of(
                    "message", "Statistics retrieved successfully",
                    "statistics", statistics
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
