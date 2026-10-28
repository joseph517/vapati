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

@DisplayName("UrlValidationService - Unit Tests")
class UrlValidationServiceTest {

    private static final String LABEL = "Cover image";
    private static final int MAX_LENGTH = 255;

    private UrlValidationService urlValidationService;

    @BeforeEach
    void setUp() {
        urlValidationService = new UrlValidationService();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t ", "   "})
    @DisplayName("Should return null when the URL is null, empty or only whitespace")
    void normalizeOptionalUrl_WhenBlank_ShouldReturnNull(String value) {
        // When
        String result = urlValidationService.normalizeOptionalUrl(value, LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return the URL without leading and trailing spaces")
    void normalizeOptionalUrl_WithSurroundingSpaces_ShouldReturnTrimmed() {
        // When
        String result = urlValidationService.normalizeOptionalUrl("  https://example.com/a  ", LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isEqualTo("https://example.com/a");
    }

    @Test
    @DisplayName("Should accept an uppercase scheme and keep the URL unchanged")
    void normalizeOptionalUrl_WithUppercaseScheme_ShouldReturnUnchanged() {
        // When
        String result = urlValidationService.normalizeOptionalUrl("HTTP://Example.com/x", LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isEqualTo("HTTP://Example.com/x");
    }

    @Test
    @DisplayName("Should accept a URL of exactly the max length")
    void normalizeOptionalUrl_WithMaxLength_ShouldReturnUrl() {
        // Given
        String url = urlOfLength(255);

        // When
        String result = urlValidationService.normalizeOptionalUrl(url, LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isEqualTo(url);
    }

    @Test
    @DisplayName("Should check the length after trimming")
    void normalizeOptionalUrl_WithMaxLengthPlusSurroundingSpaces_ShouldReturnTrimmed() {
        // Given
        String url = urlOfLength(255);

        // When
        String result = urlValidationService.normalizeOptionalUrl("  " + url + "  ", LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isEqualTo(url);
    }

    @Test
    @DisplayName("Should throw when the URL is longer than the max length")
    void normalizeOptionalUrl_OverMaxLength_ShouldThrow() {
        // Given
        String url = urlOfLength(256);

        // When & Then
        assertThatThrownBy(() -> urlValidationService.normalizeOptionalUrl(url, LABEL, MAX_LENGTH))
                .isInstanceOf(MessageException.class)
                .hasMessage("Cover image must be at most 255 characters");
    }

    @Test
    @DisplayName("Should check the length before the format")
    void normalizeOptionalUrl_OverMaxLengthAndInvalid_ShouldThrowLengthError() {
        // Given
        String value = "a".repeat(256);

        // When & Then
        assertThatThrownBy(() -> urlValidationService.normalizeOptionalUrl(value, LABEL, MAX_LENGTH))
                .isInstanceOf(MessageException.class)
                .hasMessage("Cover image must be at most 255 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "data:text/html,x",
            "mailto:a@b.com",
            "ftp://example.com",
            "example.com",
            "https://",
            "http://exa mple.com",
            "http://exa_mple.com"
    })
    @DisplayName("Should throw when the URL is not http or https with a host")
    void normalizeOptionalUrl_WhenInvalid_ShouldThrow(String value) {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.normalizeOptionalUrl(value, LABEL, MAX_LENGTH))
                .isInstanceOf(MessageException.class)
                .hasMessage("Cover image must be a valid http or https URL");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://exa\u0007mple.com",
            "https://example.com/a\u0000b",
            "https://example.com/a\nb",
            "http://ex\u00e4mple.com",
            "http://example.com:abc"
    })
    @DisplayName("Should throw when the URL has control characters, a non-ASCII host or an invalid port")
    void normalizeOptionalUrl_WithControlCharsNonAsciiHostOrInvalidPort_ShouldThrow(String value) {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.normalizeOptionalUrl(value, LABEL, MAX_LENGTH))
                .isInstanceOf(MessageException.class)
                .hasMessage("Cover image must be a valid http or https URL");
    }

    @Test
    @DisplayName("Should accept non-ASCII characters in the path and keep them unchanged")
    void normalizeOptionalUrl_WithNonAsciiPath_ShouldReturnUnchanged() {
        // When
        String result = urlValidationService.normalizeOptionalUrl("https://example.com/caf\u00e9", LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isEqualTo("https://example.com/caf\u00e9");
    }

    @Test
    @DisplayName("Should return null when the value is only Unicode whitespace (isBlank)")
    void normalizeOptionalUrl_WithOnlyUnicodeWhitespace_ShouldReturnNull() {
        // When
        String result = urlValidationService.normalizeOptionalUrl("\u2003\u2003", LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should not trim Unicode whitespace around the URL (trim only removes chars up to U+0020)")
    void normalizeOptionalUrl_WithLeadingUnicodeWhitespace_ShouldThrow() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.normalizeOptionalUrl("\u2003https://example.com", LABEL, MAX_LENGTH))
                .isInstanceOf(MessageException.class)
                .hasMessage("Cover image must be a valid http or https URL");
    }

    @Test
    @DisplayName("Should accept a URL with userinfo and keep it unchanged (its host is the part after the @)")
    void normalizeOptionalUrl_WithUserInfo_ShouldReturnUrl() {
        // When
        String result = urlValidationService.normalizeOptionalUrl("https://instagram.com@evil.com", LABEL, MAX_LENGTH);

        // Then
        assertThat(result).isEqualTo("https://instagram.com@evil.com");
    }

    @Test
    @DisplayName("Should start both error messages with the field label")
    void normalizeOptionalUrl_WithOtherLabel_ShouldStartMessagesWithLabel() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.normalizeOptionalUrl("https://example.com", "Website", 10))
                .isInstanceOf(MessageException.class)
                .hasMessage("Website must be at most 10 characters");
        assertThatThrownBy(() -> urlValidationService.normalizeOptionalUrl("example.com", "Website", MAX_LENGTH))
                .isInstanceOf(MessageException.class)
                .hasMessage("Website must be a valid http or https URL");
    }

    private static String urlOfLength(int length) {
        String prefix = "https://example.com/";
        return prefix + "a".repeat(length - prefix.length());
    }
}
