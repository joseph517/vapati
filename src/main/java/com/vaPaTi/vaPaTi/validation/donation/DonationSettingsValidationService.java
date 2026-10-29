package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.dtos.donation.UpdateDonationSettingsDTO;
import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// Rules of the donation panel PUT. Everything is checked before writing, so an error leaves the settings as they were
@Service
@RequiredArgsConstructor
public class DonationSettingsValidationService {

    private static final int MAX_UNIT_NAME_LENGTH = 30;
    private static final int MAX_SUGGESTED_AMOUNTS = 3;
    private static final int AMOUNT_SCALE = 2;

    private final TextValidationService textValidationService;

    // Full replacement: in SIMPLE the unit fields are dropped even if they were sent
    public void validateAndApply(DonationSettings settings, UpdateDonationSettingsDTO dto) {
        DonationMode mode = dto.getMode();

        String unitName = null;
        BigDecimal unitAmount = null;
        if (mode == DonationMode.UNIT) {
            unitName = textValidationService.normalizeRequiredText(dto.getUnitName(), "Unit name", MAX_UNIT_NAME_LENGTH);
            if (dto.getUnitAmount() == null) {
                throw new MessageException("Unit amount is required in UNIT mode");
            }
            unitAmount = scale(dto.getUnitAmount());
        }
        BigDecimal minimumAmount = dto.getMinimumAmount() == null ? null : scale(dto.getMinimumAmount());

        // The rule lives in DonationSettings; a detached copy computes it without touching the real settings
        BigDecimal effectiveMinimum = DonationSettings.builder()
                .mode(mode)
                .unitAmount(unitAmount)
                .minimumAmount(minimumAmount)
                .build()
                .getEffectiveMinimumAmount();

        List<BigDecimal> suggestedAmounts = validateAndSortSuggestedAmounts(dto.getSuggestedAmounts(), effectiveMinimum);

        settings.setMode(mode);
        settings.setUnitName(unitName);
        settings.setUnitAmount(unitAmount);
        settings.setMinimumAmount(minimumAmount);
        settings.replaceSuggestedAmounts(suggestedAmounts);
        settings.setCtaText(dto.getCtaText());
    }

    // null counts as []. The size and null-element checks repeat the DTO's, because the table has 3 columns
    private List<BigDecimal> validateAndSortSuggestedAmounts(List<BigDecimal> amounts, BigDecimal effectiveMinimum) {
        if (amounts == null) {
            return List.of();
        }
        if (amounts.size() > MAX_SUGGESTED_AMOUNTS) {
            throw new MessageException("At most " + MAX_SUGGESTED_AMOUNTS + " suggested amounts are allowed");
        }
        if (amounts.stream().anyMatch(Objects::isNull)) {
            throw new MessageException("Suggested amount is required");
        }

        // Scaled first, so 5 and 5.00 are the same amount
        List<BigDecimal> scaled = amounts.stream().map(this::scale).toList();
        Set<BigDecimal> distinct = new HashSet<>(scaled);
        if (distinct.size() < scaled.size()) {
            throw new MessageException("Suggested amounts must be distinct");
        }
        if (effectiveMinimum != null && scaled.stream().anyMatch(a -> a.compareTo(effectiveMinimum) < 0)) {
            throw new MessageException("Suggested amounts must be at least " + effectiveMinimum.toPlainString());
        }

        return scaled.stream().sorted().toList();
    }

    // The DTO's @Digits already limits every amount to 2 decimals, so this only pads (5 -> 5.00) and never rounds
    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(AMOUNT_SCALE);
    }
}
