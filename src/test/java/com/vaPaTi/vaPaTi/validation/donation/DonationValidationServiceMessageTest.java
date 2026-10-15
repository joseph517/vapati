package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationValidationService - Message Tests")
class DonationValidationServiceMessageTest {

    @Mock
    private CampaignValidationService campaignValidationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DonationValidationService donationValidationService;

    @Test
    @DisplayName("Should return null when there is no message")
    void validateAndNormalizeMessage_WhenNull_ShouldReturnNull() {
        // When
        String result = donationValidationService.validateAndNormalizeMessage(null);

        // Then
        assertThat(result).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n\t"})
    @DisplayName("Should throw when the message is empty or only whitespace")
    void validateAndNormalizeMessage_WhenBlank_ShouldThrow(String message) {
        // When & Then
        assertThatThrownBy(() -> donationValidationService.validateAndNormalizeMessage(message))
                .isInstanceOf(MessageException.class)
                .hasMessage("Message cannot be blank");
    }

    @Test
    @DisplayName("Should return the message without leading and trailing spaces")
    void validateAndNormalizeMessage_WithSurroundingSpaces_ShouldReturnTrimmed() {
        // When
        String result = donationValidationService.validateAndNormalizeMessage("  hola  ");

        // Then
        assertThat(result).isEqualTo("hola");
    }

    @Test
    @DisplayName("Should accept a message of exactly 280 characters")
    void validateAndNormalizeMessage_With280Characters_ShouldReturnMessage() {
        // Given
        String message = "a".repeat(280);

        // When
        String result = donationValidationService.validateAndNormalizeMessage(message);

        // Then
        assertThat(result).isEqualTo(message);
    }

    @Test
    @DisplayName("Should throw when the message has more than 280 characters")
    void validateAndNormalizeMessage_With281Characters_ShouldThrow() {
        // Given
        String message = "a".repeat(281);

        // When & Then
        assertThatThrownBy(() -> donationValidationService.validateAndNormalizeMessage(message))
                .isInstanceOf(MessageException.class)
                .hasMessage("Message must be at most 280 characters");
    }
}
