package com.vaPaTi.vaPaTi.dtos.donation;

import com.vaPaTi.vaPaTi.entity.donation.DonationCtaText;
import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.validate;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateDonationSettingsDTO - Bean Validation")
class DonationSettingsDtoValidationTest {

    private static final String DIGITS_MESSAGE = "numeric value out of bounds (<13 digits>.<2 digits> expected)";

    private UpdateDonationSettingsDTO dto;

    @BeforeEach
    void setUp() {
        dto = new UpdateDonationSettingsDTO();
        dto.setMode(DonationMode.SIMPLE);
        dto.setCtaText(DonationCtaText.DONATE);
    }

    private void assertSingleViolation(String field, String message) {
        Set<ConstraintViolation<UpdateDonationSettingsDTO>> violations = validate(dto);
        assertThat(violations).hasSize(1);
        ConstraintViolation<UpdateDonationSettingsDTO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath()).hasToString(field);
        assertThat(violation.getMessage()).isEqualTo(message);
    }

    private static List<BigDecimal> amounts(String... values) {
        return new ArrayList<>(Arrays.stream(values).map(value -> value == null ? null : new BigDecimal(value)).toList());
    }

    @Test
    @DisplayName("A minimal DTO (mode and ctaText) has no violations")
    void minimalDtoHasNoViolations() {
        assertValid(dto);
    }

    @Test
    @DisplayName("A full UNIT DTO with 3 suggested amounts and 2 decimals has no violations")
    void fullDtoHasNoViolations() {
        dto.setMode(DonationMode.UNIT);
        dto.setUnitName("a".repeat(30));
        dto.setUnitAmount(new BigDecimal("4.50"));
        dto.setMinimumAmount(new BigDecimal("3.00"));
        dto.setSuggestedAmounts(amounts("4.50", "9", "1234567890123.99"));
        dto.setCtaText(DonationCtaText.TIP);
        assertValid(dto);
    }

    @Test
    @DisplayName("An empty suggestedAmounts list has no violations")
    void emptySuggestedAmountsIsValid() {
        dto.setSuggestedAmounts(List.of());
        assertValid(dto);
    }

    @Test
    @DisplayName("A blank unitName passes Bean Validation (the UNIT rule lives in the validation service)")
    void blankUnitNameIsValid() {
        dto.setUnitName("   ");
        assertValid(dto);
    }

    @Test
    @DisplayName("mode is required")
    void modeIsRequired() {
        dto.setMode(null);
        assertSingleViolation("mode", "Mode is required");
    }

    @Test
    @DisplayName("ctaText is required")
    void ctaTextIsRequired() {
        dto.setCtaText(null);
        assertSingleViolation("ctaText", "CTA text is required");
    }

    @Test
    @DisplayName("unitName of 31 characters, spaces included, is rejected")
    void unitNameTooLong() {
        dto.setUnitName(" " + "a".repeat(29) + " ");
        assertSingleViolation("unitName", "Unit name must be at most 30 characters");
    }

    @ParameterizedTest(name = "unitAmount = {0}")
    @ValueSource(strings = {"0", "-1"})
    @DisplayName("unitAmount must be greater than zero, also in SIMPLE mode")
    void unitAmountNotPositive(String value) {
        dto.setUnitAmount(new BigDecimal(value));
        assertSingleViolation("unitAmount", "Unit amount must be greater than zero");
    }

    @ParameterizedTest(name = "unitAmount = {0}")
    @ValueSource(strings = {"1.234", "12345678901234"})
    @DisplayName("unitAmount allows at most 13 integer digits and 2 decimals")
    void unitAmountOutOfBounds(String value) {
        dto.setUnitAmount(new BigDecimal(value));
        assertSingleViolation("unitAmount", DIGITS_MESSAGE);
    }

    @ParameterizedTest(name = "minimumAmount = {0}")
    @ValueSource(strings = {"0", "-3"})
    @DisplayName("minimumAmount must be greater than zero")
    void minimumAmountNotPositive(String value) {
        dto.setMinimumAmount(new BigDecimal(value));
        assertSingleViolation("minimumAmount", "Minimum amount must be greater than zero");
    }

    @ParameterizedTest(name = "minimumAmount = {0}")
    @ValueSource(strings = {"1.234", "12345678901234"})
    @DisplayName("minimumAmount allows at most 13 integer digits and 2 decimals")
    void minimumAmountOutOfBounds(String value) {
        dto.setMinimumAmount(new BigDecimal(value));
        assertSingleViolation("minimumAmount", DIGITS_MESSAGE);
    }

    @Test
    @DisplayName("4 suggested amounts are rejected on suggestedAmounts")
    void tooManySuggestedAmounts() {
        dto.setSuggestedAmounts(amounts("1", "2", "3", "4"));
        assertSingleViolation("suggestedAmounts", "At most 3 suggested amounts are allowed");
    }

    @Test
    @DisplayName("A null suggested amount is rejected on that element")
    void nullSuggestedAmount() {
        dto.setSuggestedAmounts(amounts("5", null));
        assertSingleViolation("suggestedAmounts[1].<list element>", "Suggested amount is required");
    }

    @ParameterizedTest(name = "suggested amount = {0}")
    @ValueSource(strings = {"0", "-2"})
    @DisplayName("A suggested amount must be greater than zero, checked on that element")
    void suggestedAmountNotPositive(String value) {
        dto.setSuggestedAmounts(amounts(value));
        assertSingleViolation("suggestedAmounts[0].<list element>", "Suggested amount must be greater than zero");
    }

    @Test
    @DisplayName("A suggested amount allows at most 13 integer digits and 2 decimals")
    void suggestedAmountOutOfBounds() {
        dto.setSuggestedAmounts(amounts("1.234"));
        assertSingleViolation("suggestedAmounts[0].<list element>", DIGITS_MESSAGE);
    }
}
