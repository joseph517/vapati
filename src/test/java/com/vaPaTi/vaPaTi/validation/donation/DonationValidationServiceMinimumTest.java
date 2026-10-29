package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.donation.DonationSettingsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationValidationService - Direct donation minimum")
class DonationValidationServiceMinimumTest {

    @Mock
    private DonationSettingsRepository donationSettingsRepository;

    @InjectMocks
    private DonationValidationService donationValidationService;

    private static final Long RECIPIENT_ID = 2L;

    // Amounts as they come from the database: DECIMAL(15,2)
    private static BigDecimal amount(String value) {
        return new BigDecimal(value).setScale(2);
    }

    private void givenSettings(DonationSettings settings) {
        when(donationSettingsRepository.findByUserId(RECIPIENT_ID)).thenReturn(Optional.ofNullable(settings));
    }

    @Nested
    @DisplayName("validateDirectDonationMinimum() without an effective minimum")
    class NoMinimumTests {

        @Test
        @DisplayName("Should pass any positive amount when the recipient has no settings row")
        void validateDirectDonationMinimum_WhenNoRow_ShouldPass() {
            // Given
            givenSettings(null);

            // When & Then
            assertThatCode(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("0.01")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass any positive amount in SIMPLE without a minimum, even with a leftover unit amount")
        void validateDirectDonationMinimum_WhenSimpleWithoutMinimum_ShouldPass() {
            // Given: in SIMPLE the unit amount doesn't count
            givenSettings(DonationSettings.builder()
                    .mode(DonationMode.SIMPLE)
                    .unitAmount(amount("4"))
                    .build());

            // When & Then
            assertThatCode(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("0.01")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass any positive amount in UNIT without unit amount nor minimum")
        void validateDirectDonationMinimum_WhenUnitWithoutAmounts_ShouldPass() {
            // Given
            givenSettings(DonationSettings.builder()
                    .mode(DonationMode.UNIT)
                    .unitName("coffee")
                    .build());

            // When & Then
            assertThatCode(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("0.01")))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateDirectDonationMinimum() in SIMPLE mode")
    class SimpleTests {

        @Test
        @DisplayName("Should reject an amount below the minimum with the minimum in 2 decimals")
        void validateDirectDonationMinimum_WhenSimpleBelowMinimum_ShouldThrow() {
            // Given
            givenSettings(DonationSettings.builder()
                    .mode(DonationMode.SIMPLE)
                    .minimumAmount(amount("3"))
                    .build());

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("2.99")))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Amount must be at least 3.00");
        }

        @Test
        @DisplayName("Should accept an amount equal to the minimum, whatever its scale")
        void validateDirectDonationMinimum_WhenSimpleAtMinimum_ShouldPass() {
            // Given
            givenSettings(DonationSettings.builder()
                    .mode(DonationMode.SIMPLE)
                    .minimumAmount(amount("3"))
                    .build());

            // When & Then
            assertThatCode(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("3.00")))
                    .doesNotThrowAnyException();
            assertThatCode(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("3")))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateDirectDonationMinimum() in UNIT mode")
    class UnitTests {

        private DonationSettings unitSettings() {
            return DonationSettings.builder()
                    .mode(DonationMode.UNIT)
                    .unitName("coffee")
                    .unitAmount(amount("4"))
                    .minimumAmount(amount("3"))
                    .build();
        }

        @Test
        @DisplayName("Should reject an amount below the unit amount when it is greater than the minimum")
        void validateDirectDonationMinimum_WhenUnitBelowEffectiveMinimum_ShouldThrow() {
            // Given
            givenSettings(unitSettings());

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("3.50")))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Amount must be at least 4.00");
        }

        @Test
        @DisplayName("Should accept an amount equal to the effective minimum")
        void validateDirectDonationMinimum_WhenUnitAtEffectiveMinimum_ShouldPass() {
            // Given
            givenSettings(unitSettings());

            // When & Then
            assertThatCode(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("4")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept an amount that is not a multiple of the unit amount")
        void validateDirectDonationMinimum_WhenUnitNotMultiple_ShouldPass() {
            // Given
            givenSettings(unitSettings());

            // When & Then
            assertThatCode(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("4.50")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should use the minimum when it is greater than the unit amount")
        void validateDirectDonationMinimum_WhenUnitMinimumGreaterThanUnit_ShouldUseMinimum() {
            // Given
            givenSettings(DonationSettings.builder()
                    .mode(DonationMode.UNIT)
                    .unitName("coffee")
                    .unitAmount(amount("2"))
                    .minimumAmount(amount("5"))
                    .build());

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateDirectDonationMinimum(RECIPIENT_ID, new BigDecimal("4.99")))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Amount must be at least 5.00");
        }
    }
}
