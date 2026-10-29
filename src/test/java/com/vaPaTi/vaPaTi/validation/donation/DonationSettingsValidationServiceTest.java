package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.dtos.donation.UpdateDonationSettingsDTO;
import com.vaPaTi.vaPaTi.entity.donation.DonationCtaText;
import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationSettingsValidationService - Unit Tests")
class DonationSettingsValidationServiceTest {

    // Real instance: the unit name rule lives in TextValidationService
    @Spy
    private TextValidationService textValidationService = new TextValidationService();

    @InjectMocks
    private DonationSettingsValidationService donationSettingsValidationService;

    private DonationSettings settings;

    @BeforeEach
    void setUp() {
        settings = new DonationSettings();
    }

    private static UpdateDonationSettingsDTO dto(DonationMode mode, String unitName, String unitAmount,
                                                 String minimumAmount, String... suggestedAmounts) {
        UpdateDonationSettingsDTO dto = new UpdateDonationSettingsDTO();
        dto.setMode(mode);
        dto.setUnitName(unitName);
        dto.setUnitAmount(unitAmount == null ? null : new BigDecimal(unitAmount));
        dto.setMinimumAmount(minimumAmount == null ? null : new BigDecimal(minimumAmount));
        dto.setSuggestedAmounts(Arrays.stream(suggestedAmounts).map(BigDecimal::new).toList());
        dto.setCtaText(DonationCtaText.TIP);
        return dto;
    }

    private static List<String> plain(List<BigDecimal> amounts) {
        return amounts.stream().map(BigDecimal::toPlainString).toList();
    }

    @Nested
    @DisplayName("Mode and unit")
    class ModeAndUnit {

        @Test
        @DisplayName("Should leave the unit fields null in SIMPLE mode even if they are sent")
        void simple_WithUnitFields_ShouldDropThem() {
            settings.setUnitName("café");
            settings.setUnitAmount(new BigDecimal("2.00"));

            donationSettingsValidationService.validateAndApply(settings, dto(DonationMode.SIMPLE, "café", "2", "3"));

            assertThat(settings.getMode()).isEqualTo(DonationMode.SIMPLE);
            assertThat(settings.getUnitName()).isNull();
            assertThat(settings.getUnitAmount()).isNull();
            assertThat(settings.getMinimumAmount().toPlainString()).isEqualTo("3.00");
            assertThat(settings.getCtaText()).isEqualTo(DonationCtaText.TIP);
        }

        @Test
        @DisplayName("Should save the unit name without surrounding spaces and the unit amount with 2 decimals in UNIT mode")
        void unit_WithSurroundingSpaces_ShouldTrimUnitName() {
            donationSettingsValidationService.validateAndApply(settings, dto(DonationMode.UNIT, "  café  ", "4", null));

            assertThat(settings.getMode()).isEqualTo(DonationMode.UNIT);
            assertThat(settings.getUnitName()).isEqualTo("café");
            assertThat(settings.getUnitAmount().toPlainString()).isEqualTo("4.00");
            assertThat(settings.getMinimumAmount()).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\n\t"})
        @DisplayName("Should throw in UNIT mode when the unit name is missing or blank")
        void unit_WithBlankUnitName_ShouldThrow(String unitName) {
            UpdateDonationSettingsDTO dto = dto(DonationMode.UNIT, unitName, "4", null);

            assertThatThrownBy(() -> donationSettingsValidationService.validateAndApply(settings, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Unit name cannot be blank");
        }

        @Test
        @DisplayName("Should throw in UNIT mode when the unit amount is missing")
        void unit_WithoutUnitAmount_ShouldThrow() {
            UpdateDonationSettingsDTO dto = dto(DonationMode.UNIT, "café", null, "3");

            assertThatThrownBy(() -> donationSettingsValidationService.validateAndApply(settings, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Unit amount is required in UNIT mode");
        }
    }

    @Nested
    @DisplayName("Suggested amounts")
    class SuggestedAmounts {

        @Test
        @DisplayName("Should save the suggested amounts sorted and with 2 decimals")
        void withUnsortedAmounts_ShouldSaveSortedWithTwoDecimals() {
            donationSettingsValidationService.validateAndApply(settings, dto(DonationMode.SIMPLE, null, null, "3", "10", "5", "3.5"));

            assertThat(plain(settings.getSuggestedAmounts())).containsExactly("3.50", "5.00", "10.00");
        }

        @Test
        @DisplayName("Should throw when two suggested amounts are equal, counting 5 and 5.00 as the same")
        void withRepeatedAmounts_ShouldThrow() {
            UpdateDonationSettingsDTO dto = dto(DonationMode.SIMPLE, null, null, null, "5", "5.00");

            assertThatThrownBy(() -> donationSettingsValidationService.validateAndApply(settings, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Suggested amounts must be distinct");
        }

        @Test
        @DisplayName("Should throw when a suggested amount is below the minimum in SIMPLE mode")
        void simple_WithAmountBelowMinimum_ShouldThrow() {
            UpdateDonationSettingsDTO dto = dto(DonationMode.SIMPLE, null, null, "3", "2.99", "5");

            assertThatThrownBy(() -> donationSettingsValidationService.validateAndApply(settings, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Suggested amounts must be at least 3.00");
        }

        @Test
        @DisplayName("Should accept a suggested amount equal to the minimum")
        void simple_WithAmountEqualToMinimum_ShouldPass() {
            donationSettingsValidationService.validateAndApply(settings, dto(DonationMode.SIMPLE, null, null, "3", "3"));

            assertThat(plain(settings.getSuggestedAmounts())).containsExactly("3.00");
        }

        @Test
        @DisplayName("Should throw when a suggested amount is below the unit amount in UNIT mode, even above a lower minimum")
        void unit_WithAmountBelowUnitAmount_ShouldThrow() {
            UpdateDonationSettingsDTO dto = dto(DonationMode.UNIT, "café", "4", "3", "3");

            assertThatThrownBy(() -> donationSettingsValidationService.validateAndApply(settings, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Suggested amounts must be at least 4.00");
        }

        @Test
        @DisplayName("Should ignore a unit amount sent in SIMPLE mode when checking the suggested amounts")
        void simple_WithUnitAmountSent_ShouldNotUseItAsMinimum() {
            donationSettingsValidationService.validateAndApply(settings, dto(DonationMode.SIMPLE, null, "10", null, "1"));

            assertThat(plain(settings.getSuggestedAmounts())).containsExactly("1.00");
        }

        @Test
        @DisplayName("Should accept any positive suggested amount without an effective minimum")
        void withoutEffectiveMinimum_ShouldAcceptAnyPositiveAmount() {
            donationSettingsValidationService.validateAndApply(settings, dto(DonationMode.SIMPLE, null, null, null, "0.01", "100"));

            assertThat(plain(settings.getSuggestedAmounts())).containsExactly("0.01", "100.00");
        }

        @Test
        @DisplayName("Should leave the settings without suggested amounts when the list is null")
        void withNullList_ShouldClearSuggestedAmounts() {
            settings.replaceSuggestedAmounts(List.of(new BigDecimal("5.00")));
            UpdateDonationSettingsDTO dto = dto(DonationMode.SIMPLE, null, null, null);
            dto.setSuggestedAmounts(null);

            donationSettingsValidationService.validateAndApply(settings, dto);

            assertThat(settings.getSuggestedAmounts()).isEmpty();
        }

        @Test
        @DisplayName("Should leave the settings without suggested amounts when the list is empty")
        void withEmptyList_ShouldClearSuggestedAmounts() {
            settings.replaceSuggestedAmounts(List.of(new BigDecimal("5.00")));

            donationSettingsValidationService.validateAndApply(settings, dto(DonationMode.SIMPLE, null, null, null));

            assertThat(settings.getSuggestedAmounts()).isEmpty();
        }

        @Test
        @DisplayName("Should throw when there are more than 3 suggested amounts")
        void withFourAmounts_ShouldThrow() {
            UpdateDonationSettingsDTO dto = dto(DonationMode.SIMPLE, null, null, null, "1", "2", "3", "4");

            assertThatThrownBy(() -> donationSettingsValidationService.validateAndApply(settings, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("At most 3 suggested amounts are allowed");
        }

        @Test
        @DisplayName("Should throw when a suggested amount is null")
        void withNullElement_ShouldThrow() {
            UpdateDonationSettingsDTO dto = dto(DonationMode.SIMPLE, null, null, null);
            List<BigDecimal> amounts = new ArrayList<>();
            amounts.add(new BigDecimal("5"));
            amounts.add(null);
            dto.setSuggestedAmounts(amounts);

            assertThatThrownBy(() -> donationSettingsValidationService.validateAndApply(settings, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Suggested amount is required");
        }
    }

    @Nested
    @DisplayName("Errors leave the settings unchanged")
    class Unchanged {

        private DonationSettings saved;

        @BeforeEach
        void setUpSaved() {
            settings.setMode(DonationMode.UNIT);
            settings.setUnitName("café");
            settings.setUnitAmount(new BigDecimal("4.00"));
            settings.setMinimumAmount(new BigDecimal("3.00"));
            settings.replaceSuggestedAmounts(List.of(new BigDecimal("4.00"), new BigDecimal("8.00")));
            settings.setCtaText(DonationCtaText.DONATE);

            saved = new DonationSettings();
            saved.setMode(DonationMode.UNIT);
            saved.setUnitName("café");
            saved.setUnitAmount(new BigDecimal("4.00"));
            saved.setMinimumAmount(new BigDecimal("3.00"));
            saved.replaceSuggestedAmounts(List.of(new BigDecimal("4.00"), new BigDecimal("8.00")));
            saved.setCtaText(DonationCtaText.DONATE);
        }

        private void assertFailsAndUnchanged(UpdateDonationSettingsDTO dto) {
            assertThatThrownBy(() -> donationSettingsValidationService.validateAndApply(settings, dto))
                    .isInstanceOf(MessageException.class);

            assertThat(settings).isEqualTo(saved);
        }

        @Test
        @DisplayName("Should not change the settings when the unit name is blank")
        void withBlankUnitName_ShouldNotChange() {
            assertFailsAndUnchanged(dto(DonationMode.UNIT, "   ", "5", "1", "6"));
        }

        @Test
        @DisplayName("Should not change the settings when the unit amount is missing")
        void withoutUnitAmount_ShouldNotChange() {
            assertFailsAndUnchanged(dto(DonationMode.UNIT, "té", null, "1", "6"));
        }

        @Test
        @DisplayName("Should not change the settings when the suggested amounts are repeated")
        void withRepeatedAmounts_ShouldNotChange() {
            assertFailsAndUnchanged(dto(DonationMode.SIMPLE, null, null, "1", "6", "6.00"));
        }

        @Test
        @DisplayName("Should not change the settings when a suggested amount is below the effective minimum")
        void withAmountBelowMinimum_ShouldNotChange() {
            assertFailsAndUnchanged(dto(DonationMode.SIMPLE, null, null, "10", "6"));
        }

        @Test
        @DisplayName("Should not change the settings when there are more than 3 suggested amounts")
        void withFourAmounts_ShouldNotChange() {
            assertFailsAndUnchanged(dto(DonationMode.SIMPLE, null, null, null, "1", "2", "3", "4"));
        }
    }
}
