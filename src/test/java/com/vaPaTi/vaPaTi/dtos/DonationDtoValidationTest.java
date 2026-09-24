package com.vaPaTi.vaPaTi.dtos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertSingleViolation;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;

@DisplayName("Donation DTOs - Bean Validation")
class DonationDtoValidationTest {

    private CreateDonationDTO dto;

    @BeforeEach
    void setUp() {
        dto = new CreateDonationDTO();
        dto.setCampaignId(1L);
        dto.setAmount(new BigDecimal("10.55"));
    }

    @Test
    @DisplayName("A valid CreateDonationDTO with 2 decimals has no violations")
    void validDtoHasNoViolations() {
        assertValid(dto);
    }

    @Test
    @DisplayName("amount is required")
    void amountIsRequired() {
        dto.setAmount(null);
        assertSingleViolation(dto, "amount");
    }

    @ParameterizedTest(name = "amount = {0}")
    @ValueSource(strings = {"0", "-5", "10.555", "12345678901234"})
    @DisplayName("amount must be positive with at most 13 integer digits and 2 decimals")
    void invalidAmount(String amount) {
        dto.setAmount(new BigDecimal(amount));
        assertSingleViolation(dto, "amount");
    }
}
