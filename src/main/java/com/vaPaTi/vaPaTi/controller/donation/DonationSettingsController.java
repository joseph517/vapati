package com.vaPaTi.vaPaTi.controller.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationSettingsResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.UpdateDonationSettingsDTO;
import com.vaPaTi.vaPaTi.service.donation.DonationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class DonationSettingsController {

    private static final String DEFAULTS = "Without saved settings the defaults apply: mode=SIMPLE, unitName, unitAmount and "
            + "minimumAmount null, suggestedAmounts [] and ctaText=DONATE. ";
    // Shared by the three operations: the reads return what the PUT accepted, so they describe the same rules
    private static final String FIELD_RULES = "mode and ctaText are always required. In UNIT mode unitName (stored "
            + "trimmed, at most 30 characters) and unitAmount are required; in SIMPLE both are ignored and stored as null. "
            + "Amounts must be greater than zero with up to 2 decimals. suggestedAmounts: null or [] means none, at most 3, "
            + "no null items, distinct (5 and 5.00 are equal), each at least the effective minimum, stored and returned "
            + "from lowest to highest. ";
    private static final String EFFECTIVE_MINIMUM = "effectiveMinimumAmount is what a direct donation to the user must reach: "
            + "in SIMPLE it is minimumAmount, in UNIT the greater of unitAmount and minimumAmount, and null means any amount "
            + "greater than zero. Only direct donations use it, donations to campaigns do not. ";

    private final DonationSettingsService donationSettingsService;

    public DonationSettingsController(DonationSettingsService donationSettingsService) {
        this.donationSettingsService = donationSettingsService;
    }

    @GetMapping("/me/donation-settings")
    @Operation(summary = "Get my donation settings",
            description = "Any authenticated user (401 without a token). Returns the caller's donation panel settings. "
                    + FIELD_RULES + DEFAULTS + EFFECTIVE_MINIMUM)
    public ResponseEntity<DonationSettingsResponseDTO> getMyDonationSettings() {
        return ResponseEntity.ok(donationSettingsService.getMyDonationSettings());
    }

    @PutMapping("/me/donation-settings")
    @Operation(summary = "Save my donation settings",
            description = "Any authenticated user (401 without a token), only for their own panel. Full replacement: the first "
                    + "save creates the settings, the next ones update them. "
                    + FIELD_RULES + DEFAULTS + EFFECTIVE_MINIMUM)
    public ResponseEntity<DonationSettingsResponseDTO> updateMyDonationSettings(
            @Valid @RequestBody UpdateDonationSettingsDTO dto) {
        return ResponseEntity.ok(donationSettingsService.updateMyDonationSettings(dto));
    }

    @GetMapping("/{userId}/donation-settings")
    @Operation(summary = "Get the donation settings of a user",
            description = "Public, also without a token. Every field is public, so everyone gets the same response. "
                    + "A deleted or missing user is 404. A banned or suspended user is 404 too, except for the user "
                    + "themselves and any ADMIN. " + FIELD_RULES + DEFAULTS + EFFECTIVE_MINIMUM)
    public ResponseEntity<DonationSettingsResponseDTO> getDonationSettings(@PathVariable Long userId) {
        return ResponseEntity.ok(donationSettingsService.getDonationSettings(userId));
    }
}
