package com.vaPaTi.vaPaTi.validation.common;

import com.vaPaTi.vaPaTi.exception.MessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TextValidationService - Unit Tests")
class TextValidationServiceTest {

    private static final String LABEL = "Thanks message";
    private static final int MAX_LENGTH = 280;

    private TextValidationService textValidationService;

    @BeforeEach
    void setUp() {
        textValidationService = new TextValidationService();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    @DisplayName("Should throw when the text is null, empty or only whitespace")
    void normalizeRequiredText_WhenBlank_ShouldThrow(String text) {
        // When & Then
        assertThatThrownBy(() -> textValidationService.normalizeRequiredText(text, LABEL, MAX_LENGTH))
                .isInstanceOf(MessageException.class)
                .hasMessage("Thanks message cannot be blank");
    }

    @Test
    @DisplayName("Should return the text without leading and trailing spaces")
    void normalizeRequiredText_WithSurroundingSpaces_ShouldReturnTrimmed() {
        // When
        String result = textValidationService.normalizeRequiredText("  hola  ", LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isEqualTo("hola");
    }

    @Test
    @DisplayName("Should accept a text of exactly the max length")
    void normalizeRequiredText_WithMaxLength_ShouldReturnText() {
        // Given
        String text = "a".repeat(280);

        // When
        String result = textValidationService.normalizeRequiredText(text, LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isEqualTo(text);
    }

    @Test
    @DisplayName("Should throw when the text is longer than the max length")
    void normalizeRequiredText_OverMaxLength_ShouldThrow() {
        // Given
        String text = "a".repeat(281);

        // When & Then
        assertThatThrownBy(() -> textValidationService.normalizeRequiredText(text, LABEL, MAX_LENGTH))
                .isInstanceOf(MessageException.class)
                .hasMessage("Thanks message must be at most 280 characters");
    }

    @Test
    @DisplayName("Should check the length after trimming")
    void normalizeRequiredText_WithMaxLengthPlusSurroundingSpaces_ShouldReturnTrimmed() {
        // Given
        String text = "a".repeat(280);

        // When
        String result = textValidationService.normalizeRequiredText("  " + text + "  ", LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isEqualTo(text);
    }
}
