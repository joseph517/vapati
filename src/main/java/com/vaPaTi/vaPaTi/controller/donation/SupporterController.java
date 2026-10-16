package com.vaPaTi.vaPaTi.controller.donation;

import com.vaPaTi.vaPaTi.dtos.donation.SupporterResponseDTO;
import com.vaPaTi.vaPaTi.service.donation.SupporterService;
import com.vaPaTi.vaPaTi.validation.common.SortValidationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}")
public class SupporterController {

    private static final List<String> SUPPORTER_SORT_FIELDS = List.of("totalAmount", "lastDonationAt");

    private final SupporterService supporterService;
    private final SortValidationService sortValidationService;

    public SupporterController(
            SupporterService supporterService,
            SortValidationService sortValidationService
    ) {
        this.supporterService = supporterService;
        this.sortValidationService = sortValidationService;
    }

    @GetMapping("/supporters")
    @Operation(summary = "Get supporters of a creator",
            description = "Public, paginated. One row per donor with the aggregates of their COMPLETED donations to the campaigns of {userId}. "
                    + "Everyone gets donorUserId and donorUserName. Only the creator themselves and any ADMIN also get totalAmount, "
                    + "donationsCount and lastDonationAt; anyone else, with or without a token, gets them as null. "
                    + "sortBy=lastDonationAt is only allowed for the creator or an ADMIN (400 otherwise). Ties are broken by donorUserId ascending")
    public ResponseEntity<Page<SupporterResponseDTO>> getSupporters(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "totalAmount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Pageable pageable = sortValidationService.validateAndGetPageable(
                page, size, sortBy, sortDirection, SUPPORTER_SORT_FIELDS);
        return ResponseEntity.ok(supporterService.getSupporters(userId, pageable));
    }
}
