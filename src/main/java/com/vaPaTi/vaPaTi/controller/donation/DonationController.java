package com.vaPaTi.vaPaTi.controller.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CampaignStatisticsDTO;
import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationThanksDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationCreatedResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.service.donation.DonationService;
import com.vaPaTi.vaPaTi.validation.common.SortValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private static final List<String> RECEIVED_SORT_FIELDS = List.of("createdAt", "amount");

    private final DonationService donationService;
    private final SortValidationService sortValidationService;

    @PostMapping
    @Operation(summary = "Create a donation to a campaign or directly to a user",
            description = "The body carries exactly one of campaignId (donation to a campaign) or recipientUserId (direct donation "
                    + "to a user, which touches no campaign); neither or both is a 400. autoThanksMessage is the recipient's "
                    + "current default thanks (the campaign owner for a campaign donation), or null if not set. "
                    + "It is not stored in the donation")
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

    @PostMapping("/{donationId}/thanks")
    @Operation(summary = "Reply to a donation with a thanks message",
            description = "Only the campaign owner (not an ADMIN), once per COMPLETED donation. thanksPrivate defaults to false. "
                    + "409 if the donation already has a reply: it is not editable, delete it to reply again")
    public ResponseEntity<Map<String, Object>> thankDonation(@PathVariable Long donationId,
                                                             @Valid @RequestBody CreateDonationThanksDTO dto) {
        DonationResponseDTO donation = donationService.thankDonation(donationId, dto);

        return ResponseEntity.ok(Map.of(
                "message", "Thanks sent successfully",
                "donation", donation
        ));
    }

    @DeleteMapping("/{donationId}/thanks")
    @Operation(summary = "Delete the thanks message of a donation",
            description = "The campaign owner or an ADMIN. Afterwards the owner can reply again")
    public ResponseEntity<Map<String, Object>> deleteDonationThanks(@PathVariable Long donationId) {
        DonationResponseDTO donation = donationService.deleteDonationThanks(donationId);

        return ResponseEntity.ok(Map.of(
                "message", "Thanks deleted successfully",
                "donation", donation
        ));
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

    @GetMapping("/received")
    @Operation(summary = "Get the donations received by the authenticated user",
            description = "Paginated. Direct donations and donations to the user's campaigns (deleted ones included, with their name), "
                    + "in every status. Nothing is hidden: transactionId, private messages and private replies are included. "
                    + "sortBy: createdAt or amount. Ties are broken by id descending")
    public ResponseEntity<Page<DonationResponseDTO>> getReceivedDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Pageable pageable = sortValidationService.validateAndGetPageable(
                page, size, sortBy, sortDirection, RECEIVED_SORT_FIELDS);
        return ResponseEntity.ok(donationService.getDonationsReceived(pageable));
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
