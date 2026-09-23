package com.vaPaTi.vaPaTi.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertSingleViolation;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;

@DisplayName("Category DTOs - Bean Validation")
class CategoryDtoValidationTest {

    private static CreateCategoryDTO createDto(String name, String description) {
        CreateCategoryDTO dto = new CreateCategoryDTO();
        dto.setName(name);
        dto.setDescription(description);
        return dto;
    }

    private static UpdateCategoryDTO updateDto(String name, String description) {
        UpdateCategoryDTO dto = new UpdateCategoryDTO();
        dto.setName(name);
        dto.setDescription(description);
        return dto;
    }

    @Nested
    @DisplayName("CreateCategoryDTO")
    class CreateCategoryTests {

        @Test
        @DisplayName("A valid DTO has no violations; description is optional")
        void validDtoHasNoViolations() {
            assertValid(createDto("Health", "Medical campaigns"));
            assertValid(createDto("Health", null));
        }

        @ParameterizedTest(name = "name = ''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("name is required and can't be blank")
        void nameIsRequired(String name) {
            assertSingleViolation(createDto(name, "Medical campaigns"), "name");
        }

        @Test
        @DisplayName("name and description reject 256 characters and accept 255")
        void maxLengths() {
            assertSingleViolation(createDto("a".repeat(256), null), "name");
            assertSingleViolation(createDto("Health", "a".repeat(256)), "description");
            assertValid(createDto("a".repeat(255), "a".repeat(255)));
        }
    }

    @Nested
    @DisplayName("UpdateCategoryDTO")
    class UpdateCategoryTests {

        @Test
        @DisplayName("An empty DTO has no violations: name null keeps the current name")
        void emptyDtoHasNoViolations() {
            assertValid(new UpdateCategoryDTO());
            assertValid(updateDto(null, "Only the description changes"));
        }

        @Test
        @DisplayName("A sent name with text is accepted")
        void nameWithTextIsAccepted() {
            assertValid(updateDto("Health", null));
            assertValid(updateDto(" Health ", null));
        }

        @ParameterizedTest(name = "name = ''{0}''")
        @ValueSource(strings = {"", "   "})
        @DisplayName("A sent name can't be empty or blank")
        void blankNameIsRejected(String name) {
            assertSingleViolation(updateDto(name, null), "name");
        }

        @Test
        @DisplayName("name and description reject 256 characters and accept 255")
        void maxLengths() {
            assertSingleViolation(updateDto("a".repeat(256), null), "name");
            assertSingleViolation(updateDto(null, "a".repeat(256)), "description");
            assertValid(updateDto("a".repeat(255), "a".repeat(255)));
        }
    }
}
