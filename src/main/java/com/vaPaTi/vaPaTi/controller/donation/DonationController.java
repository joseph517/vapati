package com.vaPaTi.vaPaTi.controller.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CampaignStatisticsDTO;
import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationCreatedResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.service.donation.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Donation management endpoints")
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    @Operation(summary = "Create a donation to a campaign",
            description = "autoThanksMessage is the campaign owner's current default thanks, or null if not set. It is not stored in the donation")
    public ResponseEntity<Map<String, Object>> createDonation(@Valid @RequestBody CreateDonationDTO dto) {
        DonationCreatedResponseDTO created = donationService.createDonation(dto);

        // Map.of rejects null values, and autoThanksMessage is null when the owner has not set one
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Donation completed successfully");
        body.put("donation", created.getDonation());
        body.put("status", "COMPLETED");
        body.put("autoThanksMessage", created.getAutoThanksMessage());

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/my-donations")
    @Operation(summary = "Get all donations made by authenticated user")
    public ResponseEntity<Map<String, Object>> getMyDonations() {
        List<DonationResponseDTO> donations = donationService.getDonationsByAuthenticatedUser();

        return ResponseEntity.ok(Map.of(
                "message", "Donations retrieved successfully",
                "donations", donations,
                "total", donations.size()
        ));
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "Get all donations for a specific campaign")
    public ResponseEntity<Map<String, Object>> getDonationsByCampaign(@PathVariable Long campaignId) {
        List<DonationResponseDTO> donations = donationService.getDonationsByCampaign(campaignId);

        return ResponseEntity.ok(Map.of(
                "message", "Campaign donations retrieved successfully",
                "donations", donations,
                "total", donations.size()
        ));
    }

    @GetMapping("/campaign/{campaignId}/statistics")
    @Operation(summary = "Get donation statistics for a campaign")
    public ResponseEntity<Map<String, Object>> getCampaignStatistics(@PathVariable Long campaignId) {
        CampaignStatisticsDTO statistics = donationService.getCampaignStatistics(campaignId);

        return ResponseEntity.ok(Map.of(
                "message", "Statistics retrieved successfully",
                "statistics", statistics
        ));
    }
}
