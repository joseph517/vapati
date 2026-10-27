package com.vaPaTi.vaPaTi.dtos.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertSingleViolation;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.validate;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Comment DTOs - Bean Validation")
class CommentDtoValidationTest {

    private CreateCommentDTO dto;

    @BeforeEach
    void setUp() {
        dto = new CreateCommentDTO();
        dto.setText("Great campaign");
    }

    @Test
    @DisplayName("A valid CreateCommentDTO has no violations")
    void validDtoHasNoViolations() {
        assertValid(dto);
    }

    @Test
    @DisplayName("A text of exactly 280 characters has no violations")
    void text280CharactersIsValid() {
        dto.setText("a".repeat(280));
        assertValid(dto);
    }

    @Test
    @DisplayName("A text of 281 characters violates text with the size message")
    void text281CharactersIsRejected() {
        dto.setText("a".repeat(281));
        assertSingleViolation(dto, "text");
        assertThat(validate(dto)).extracting(v -> v.getMessage())
                .containsExactly("Text must be at most 280 characters");
    }

    @Test
    @DisplayName("A null text has no violations (the service rejects it)")
    void nullTextHasNoViolations() {
        dto.setText(null);
        assertValid(dto);
    }

    @Test
    @DisplayName("A blank text has no violations (the service rejects it)")
    void blankTextHasNoViolations() {
        dto.setText("   ");
        assertValid(dto);
    }
}
