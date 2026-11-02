package com.vaPaTi.vaPaTi.entity.donation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DonationSettings - Unit Tests")
class DonationSettingsTest {

    private static DonationSettings settings(DonationMode mode, BigDecimal unitAmount, BigDecimal minimumAmount) {
        return DonationSettings.builder()
                .mode(mode)
                .unitAmount(unitAmount)
                .minimumAmount(minimumAmount)
                .build();
    }

    @Nested
    @DisplayName("getEffectiveMinimumAmount")
    class EffectiveMinimumAmount {

        @Test
        @DisplayName("Should return the minimum amount in SIMPLE mode")
        void simple_WithMinimum_ShouldReturnMinimum() {
            DonationSettings settings = settings(DonationMode.SIMPLE, null, new BigDecimal("3.00"));

            assertThat(settings.getEffectiveMinimumAmount()).isEqualByComparingTo("3.00");
        }

        @Test
        @DisplayName("Should return null in SIMPLE mode without a minimum")
        void simple_WithoutMinimum_ShouldReturnNull() {
            DonationSettings settings = settings(DonationMode.SIMPLE, null, null);

            assertThat(settings.getEffectiveMinimumAmount()).isNull();
        }

        @Test
        @DisplayName("Should ignore a unit amount in SIMPLE mode")
        void simple_WithUnitAmount_ShouldIgnoreIt() {
            DonationSettings withMinimum = settings(DonationMode.SIMPLE, new BigDecimal("5.00"), new BigDecimal("3.00"));
            DonationSettings withoutMinimum = settings(DonationMode.SIMPLE, new BigDecimal("5.00"), null);

            assertThat(withMinimum.getEffectiveMinimumAmount()).isEqualByComparingTo("3.00");
            assertThat(withoutMinimum.getEffectiveMinimumAmount()).isNull();
        }

        @Test
        @DisplayName("Should return the unit amount in UNIT mode when it is greater than the minimum")
        void unit_WithUnitGreaterThanMinimum_ShouldReturnUnitAmount() {
            DonationSettings settings = settings(DonationMode.UNIT, new BigDecimal("4.00"), new BigDecimal("3.00"));

            assertThat(settings.getEffectiveMinimumAmount()).isEqualByComparingTo("4.00");
        }

        @Test
        @DisplayName("Should return the minimum amount in UNIT mode when it is greater than the unit")
        void unit_WithMinimumGreaterThanUnit_ShouldReturnMinimum() {
            DonationSettings settings = settings(DonationMode.UNIT, new BigDecimal("2.00"), new BigDecimal("3.50"));

            assertThat(settings.getEffectiveMinimumAmount()).isEqualByComparingTo("3.50");
        }

        @Test
        @DisplayName("Should return the unit amount in UNIT mode without a minimum")
        void unit_WithoutMinimum_ShouldReturnUnitAmount() {
            DonationSettings settings = settings(DonationMode.UNIT, new BigDecimal("4.00"), null);

            assertThat(settings.getEffectiveMinimumAmount()).isEqualByComparingTo("4.00");
        }

        @Test
        @DisplayName("Should return the minimum amount in UNIT mode without a unit amount")
        void unit_WithoutUnitAmount_ShouldReturnMinimum() {
            DonationSettings settings = settings(DonationMode.UNIT, null, new BigDecimal("3.00"));

            assertThat(settings.getEffectiveMinimumAmount()).isEqualByComparingTo("3.00");
        }

        @Test
        @DisplayName("Should return null in UNIT mode without a unit amount nor a minimum")
        void unit_WithBothNull_ShouldReturnNull() {
            DonationSettings settings = settings(DonationMode.UNIT, null, null);

            assertThat(settings.getEffectiveMinimumAmount()).isNull();
        }
    }

    @Nested
    @DisplayName("replaceSuggestedAmounts and getSuggestedAmounts")
    class SuggestedAmounts {

        @Test
        @DisplayName("Should clear every column when replaced with no amounts")
        void replace_WithNoAmounts_ShouldLeaveAllNull() {
            DonationSettings settings = new DonationSettings();
            settings.replaceSuggestedAmounts(List.of(new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00")));

            settings.replaceSuggestedAmounts(List.of());

            assertThat(settings).extracting("suggestedAmount1", "suggestedAmount2", "suggestedAmount3")
                    .containsOnlyNulls();
            assertThat(settings.getSuggestedAmounts()).isEmpty();
        }

        @Test
        @DisplayName("Should fill only the first column with one amount")
        void replace_WithOneAmount_ShouldFillFirstColumn() {
            DonationSettings settings = new DonationSettings();
            settings.replaceSuggestedAmounts(List.of(new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00")));

            settings.replaceSuggestedAmounts(List.of(new BigDecimal("5.00")));

            assertThat(settings).extracting("suggestedAmount1", "suggestedAmount2", "suggestedAmount3")
                    .containsExactly(new BigDecimal("5.00"), null, null);
        }

        @Test
        @DisplayName("Should fill the first two columns with two amounts")
        void replace_WithTwoAmounts_ShouldFillFirstTwoColumns() {
            DonationSettings settings = new DonationSettings();
            settings.replaceSuggestedAmounts(List.of(new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00")));

            settings.replaceSuggestedAmounts(List.of(new BigDecimal("5.00"), new BigDecimal("10.00")));

            assertThat(settings).extracting("suggestedAmount1", "suggestedAmount2", "suggestedAmount3")
                    .containsExactly(new BigDecimal("5.00"), new BigDecimal("10.00"), null);
        }

        @Test
        @DisplayName("Should fill the three columns with three amounts")
        void replace_WithThreeAmounts_ShouldFillAllColumns() {
            DonationSettings settings = new DonationSettings();

            settings.replaceSuggestedAmounts(List.of(new BigDecimal("3.00"), new BigDecimal("5.00"), new BigDecimal("10.00")));

            assertThat(settings).extracting("suggestedAmount1", "suggestedAmount2", "suggestedAmount3")
                    .containsExactly(new BigDecimal("3.00"), new BigDecimal("5.00"), new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("Should return the suggested amounts in column order")
        void getSuggestedAmounts_ShouldKeepColumnOrder() {
            DonationSettings settings = new DonationSettings();
            settings.replaceSuggestedAmounts(List.of(new BigDecimal("4.00"), new BigDecimal("8.00"), new BigDecimal("12.00")));

            assertThat(settings.getSuggestedAmounts())
                    .containsExactly(new BigDecimal("4.00"), new BigDecimal("8.00"), new BigDecimal("12.00"));
        }

        @Test
        @DisplayName("Should return only the filled columns, in order")
        void getSuggestedAmounts_WithTwoAmounts_ShouldReturnTwo() {
            DonationSettings settings = new DonationSettings();
            settings.replaceSuggestedAmounts(List.of(new BigDecimal("4.00"), new BigDecimal("8.00")));

            assertThat(settings.getSuggestedAmounts())
                    .containsExactly(new BigDecimal("4.00"), new BigDecimal("8.00"));
        }
    }

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("Should default to SIMPLE, DONATE, no user and no amounts with the builder")
        void builder_ShouldUseDefaults() {
            assertDefaults(DonationSettings.builder().build());
        }

        @Test
        @DisplayName("Should default to SIMPLE, DONATE, no user and no amounts with the no-args constructor")
        void noArgsConstructor_ShouldUseDefaults() {
            assertDefaults(new DonationSettings());
        }

        private void assertDefaults(DonationSettings settings) {
            assertThat(settings.getMode()).isEqualTo(DonationMode.SIMPLE);
            assertThat(settings.getCtaText()).isEqualTo(DonationCtaText.DONATE);
            assertThat(settings.getUser()).isNull();
            assertThat(settings.getUnitName()).isNull();
            assertThat(settings.getUnitAmount()).isNull();
            assertThat(settings.getMinimumAmount()).isNull();
            assertThat(settings.getEffectiveMinimumAmount()).isNull();
            assertThat(settings.getSuggestedAmounts()).isEmpty();
        }
    }
}
