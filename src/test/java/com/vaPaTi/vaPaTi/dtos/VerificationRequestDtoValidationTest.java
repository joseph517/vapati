package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.validation.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertSingleViolation;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertViolationsOn;

@DisplayName("Verification request DTOs - Bean Validation")
class VerificationRequestDtoValidationTest {

    @Nested
    @DisplayName("CreateVerificationRequestDTO")
    class CreateVerificationRequestTests {

        private CreateVerificationRequestDTO dto;

        @BeforeEach
        void setUp() {
            dto = new CreateVerificationRequestDTO();
            dto.setDniFront("https://cdn.example.com/dni-front.jpg");
            dto.setDniBack("https://cdn.example.com/dni-back.jpg");
            dto.setSelfieUser("https://cdn.example.com/selfie.jpg");
        }

        static Stream<Arguments> imageFields() {
            return Stream.of(
                    Arguments.of("dniFront", (BiConsumer<CreateVerificationRequestDTO, String>) CreateVerificationRequestDTO::setDniFront),
                    Arguments.of("dniBack", (BiConsumer<CreateVerificationRequestDTO, String>) CreateVerificationRequestDTO::setDniBack),
                    Arguments.of("selfieUser", (BiConsumer<CreateVerificationRequestDTO, String>) CreateVerificationRequestDTO::setSelfieUser)
            );
        }

        @Test
        @DisplayName("A valid DTO has no violations; userId has no constraints (P06)")
        void validDtoHasNoViolations() {
            assertValid(dto);
        }

        @Test
        @DisplayName("An empty DTO has a violation on each image field")
        void emptyDto() {
            assertViolationsOn(new CreateVerificationRequestDTO(), "dniFront", "dniBack", "selfieUser");
        }

        @ParameterizedTest(name = "missing {0}")
        @MethodSource("imageFields")
        @DisplayName("Each image field is required")
        void missingField(String field, BiConsumer<CreateVerificationRequestDTO, String> setter) {
            setter.accept(dto, null);
            assertSingleViolation(dto, field);
        }

        @ParameterizedTest(name = "blank {0}")
        @MethodSource("imageFields")
        @DisplayName("Each image field can't be blank")
        void blankField(String field, BiConsumer<CreateVerificationRequestDTO, String> setter) {
            setter.accept(dto, "   ");
            assertSingleViolation(dto, field);
        }

        @ParameterizedTest(name = "{0} with 256 characters")
        @MethodSource("imageFields")
        @DisplayName("Each image field rejects 256 characters (O10)")
        void fieldTooLong(String field, BiConsumer<CreateVerificationRequestDTO, String> setter) {
            setter.accept(dto, "a".repeat(256));
            assertSingleViolation(dto, field);
        }

        @ParameterizedTest(name = "{0} with 255 characters")
        @MethodSource("imageFields")
        @DisplayName("Each image field accepts 255 characters")
        void fieldAtLimit(String field, BiConsumer<CreateVerificationRequestDTO, String> setter) {
            setter.accept(dto, "a".repeat(255));
            assertValid(dto);
        }
    }

    @Nested
    @DisplayName("ProcessVerificationRequestDTO")
    class ProcessVerificationRequestTests {

        private ProcessVerificationRequestDTO dto;

        @BeforeEach
        void setUp() {
            dto = new ProcessVerificationRequestDTO();
            dto.setRequestId(1L);
            dto.setStatus(VerificationStatus.APPROVED);
        }

        @Test
        @DisplayName("A valid DTO has no violations")
        void validDtoHasNoViolations() {
            assertValid(dto);
        }

        @Test
        @DisplayName("An empty DTO has violations on requestId and status")
        void emptyDto() {
            assertViolationsOn(new ProcessVerificationRequestDTO(), "requestId", "status");
        }

        @ParameterizedTest(name = "requestId = {0}")
        @ValueSource(longs = {0L, -1L})
        @DisplayName("requestId must be positive")
        void requestIdMustBePositive(long requestId) {
            dto.setRequestId(requestId);
            assertSingleViolation(dto, "requestId");
        }

        @Test
        @DisplayName("status is required")
        void statusIsRequired() {
            dto.setStatus(null);
            assertSingleViolation(dto, "status");
        }
    }
}
