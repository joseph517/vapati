package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.ReportReason;
import com.vaPaTi.vaPaTi.entity.ReportStatus;
import com.vaPaTi.vaPaTi.entity.ReportedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertSingleViolation;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertViolationsOn;

@DisplayName("Report DTOs - Bean Validation")
class ReportDtoValidationTest {

    @Nested
    @DisplayName("CreateReportDTO")
    class CreateReportTests {

        private CreateReportDTO dto;

        @BeforeEach
        void setUp() {
            dto = CreateReportDTO.builder()
                    .reportedEntityType(ReportedEntityType.CAMPAIGN)
                    .reportedEntityId(20002L)
                    .reason(ReportReason.FRAUD)
                    .description("Looks fake")
                    .build();
        }

        @Test
        @DisplayName("A valid DTO has no violations; description is optional")
        void validDtoHasNoViolations() {
            assertValid(dto);
            dto.setDescription(null);
            assertValid(dto);
        }

        @Test
        @DisplayName("An empty DTO has violations on reportedEntityType, reportedEntityId and reason")
        void emptyDto() {
            assertViolationsOn(new CreateReportDTO(), "reportedEntityType", "reportedEntityId", "reason");
        }

        @Test
        @DisplayName("reportedEntityType is required")
        void reportedEntityTypeIsRequired() {
            dto.setReportedEntityType(null);
            assertSingleViolation(dto, "reportedEntityType");
        }

        @Test
        @DisplayName("reportedEntityId is required")
        void reportedEntityIdIsRequired() {
            dto.setReportedEntityId(null);
            assertSingleViolation(dto, "reportedEntityId");
        }

        @ParameterizedTest(name = "reportedEntityId = {0}")
        @ValueSource(longs = {0L, -1L})
        @DisplayName("reportedEntityId must be positive")
        void reportedEntityIdMustBePositive(long id) {
            dto.setReportedEntityId(id);
            assertSingleViolation(dto, "reportedEntityId");
        }

        @Test
        @DisplayName("reason is required")
        void reasonIsRequired() {
            dto.setReason(null);
            assertSingleViolation(dto, "reason");
        }

        @Test
        @DisplayName("description rejects 1001 characters and accepts 1000")
        void descriptionMaxLength() {
            dto.setDescription("a".repeat(1001));
            assertSingleViolation(dto, "description");

            dto.setDescription("a".repeat(1000));
            assertValid(dto);
        }
    }

    @Nested
    @DisplayName("ReviewReportDTO")
    class ReviewReportTests {

        @Test
        @DisplayName("A valid DTO has no violations; adminNotes and actionTaken are optional")
        void validDtoHasNoViolations() {
            assertValid(ReviewReportDTO.builder().status(ReportStatus.RESOLVED).build());
        }

        @Test
        @DisplayName("status is required")
        void statusIsRequired() {
            assertSingleViolation(new ReviewReportDTO(), "status");
        }
    }
}
