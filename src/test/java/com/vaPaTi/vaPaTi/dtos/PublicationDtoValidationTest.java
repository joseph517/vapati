package com.vaPaTi.vaPaTi.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertSingleViolation;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;

@DisplayName("Publication DTOs - Bean Validation")
class PublicationDtoValidationTest {

    @Test
    @DisplayName("A valid CreatePublicationDTO has no violations")
    void validDtoHasNoViolations() {
        assertValid(new CreatePublicationDTO("My first update"));
    }

    @ParameterizedTest(name = "description = ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("description is required and can't be blank")
    void descriptionIsRequired(String description) {
        assertSingleViolation(new CreatePublicationDTO(description), "description");
    }

    @Test
    @DisplayName("description rejects 256 characters and accepts 255")
    void descriptionMaxLength() {
        assertSingleViolation(new CreatePublicationDTO("a".repeat(256)), "description");
        assertValid(new CreatePublicationDTO("a".repeat(255)));
    }
}
