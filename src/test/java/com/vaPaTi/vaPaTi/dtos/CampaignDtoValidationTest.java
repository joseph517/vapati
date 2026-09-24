package com.vaPaTi.vaPaTi.dtos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertSingleViolation;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertViolationsOn;

@DisplayName("Campaign DTOs - Bean Validation")
class CampaignDtoValidationTest {

    @Nested
    @DisplayName("CreateCampaignRequestDTO")
    class CreateCampaignTests {

        private CreateCampaignRequestDTO dto;

        @BeforeEach
        void setUp() {
            dto = new CreateCampaignRequestDTO("School supplies", "Help us buy notebooks", new BigDecimal("1000.0"), null, List.of(1L));
        }

        @Test
        @DisplayName("A valid DTO has no violations; an empty description is accepted")
        void validDtoHasNoViolations() {
            assertValid(dto);
            dto.setDescription("");
            assertValid(dto);
        }

        @Test
        @DisplayName("Without name and amountGoal, both are violations")
        void missingNameAndAmountGoal() {
            dto.setName(null);
            dto.setAmountGoal(null);
            assertViolationsOn(dto, "name", "amountGoal");
        }

        @ParameterizedTest(name = "name = ''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("name is required and can't be blank")
        void nameIsRequired(String name) {
            dto.setName(name);
            assertSingleViolation(dto, "name");
        }

        @Test
        @DisplayName("description is required")
        void descriptionIsRequired() {
            dto.setDescription(null);
            assertSingleViolation(dto, "description");
        }

        @Test
        @DisplayName("amountGoal is required")
        void amountGoalIsRequired() {
            dto.setAmountGoal(null);
            assertSingleViolation(dto, "amountGoal");
        }

        @ParameterizedTest(name = "amountGoal = {0}")
        @ValueSource(strings = {"0", "-1", "10.555"})
        @DisplayName("amountGoal must be positive with at most 2 decimals")
        void invalidAmountGoal(String amountGoal) {
            dto.setAmountGoal(new BigDecimal(amountGoal));
            assertSingleViolation(dto, "amountGoal");
        }

        @Test
        @DisplayName("amountGoal accepts 2 decimals")
        void amountGoalWithTwoDecimals() {
            dto.setAmountGoal(new BigDecimal("10.55"));
            assertValid(dto);
        }

        @Test
        @DisplayName("name and description reject 256 characters and accept 255")
        void maxLengths() {
            dto.setName("a".repeat(256));
            assertSingleViolation(dto, "name");

            dto.setName("a".repeat(255));
            dto.setDescription("a".repeat(256));
            assertSingleViolation(dto, "description");

            dto.setDescription("a".repeat(255));
            assertValid(dto);
        }

        @Test
        @DisplayName("categoryIds keeps its existing constraints: 1 to 5 elements")
        void categoryIdsConstraints() {
            dto.setCategoryIds(List.of());
            assertViolationsOn(dto, "categoryIds", "categoryIds");

            dto.setCategoryIds(List.of(1L, 2L, 3L, 4L, 5L, 6L));
            assertSingleViolation(dto, "categoryIds");
        }
    }

    @Nested
    @DisplayName("UpdateCampaignRequestDTO")
    class UpdateCampaignTests {

        private UpdateCampaignRequestDTO dto;

        @BeforeEach
        void setUp() {
            dto = new UpdateCampaignRequestDTO(null, null, null, List.of(1L));
        }

        @Test
        @DisplayName("name, description and amountGoal are optional")
        void optionalFields() {
            assertValid(dto);
        }

        @Test
        @DisplayName("name and description reject 256 characters and accept 255")
        void maxLengths() {
            dto.setName("a".repeat(256));
            assertSingleViolation(dto, "name");

            dto.setName("a".repeat(255));
            dto.setDescription("a".repeat(256));
            assertSingleViolation(dto, "description");

            dto.setDescription("a".repeat(255));
            assertValid(dto);
        }

        @ParameterizedTest(name = "amountGoal = {0}")
        @ValueSource(strings = {"0", "-1", "10.555"})
        @DisplayName("amountGoal, when present, must be positive with at most 2 decimals")
        void invalidAmountGoal(String amountGoal) {
            dto.setAmountGoal(new BigDecimal(amountGoal));
            assertSingleViolation(dto, "amountGoal");
        }

        @Test
        @DisplayName("amountGoal accepts 2 decimals")
        void amountGoalWithTwoDecimals() {
            dto.setAmountGoal(new BigDecimal("10.55"));
            assertValid(dto);
        }

        @ParameterizedTest(name = "name = ''{0}''")
        @ValueSource(strings = {"", "   "})
        @DisplayName("name, when present, can't be blank")
        void blankName(String name) {
            dto.setName(name);
            assertSingleViolation(dto, "name");
        }
    }
}
