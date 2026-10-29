package com.vaPaTi.vaPaTi.mapper.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationSettingsResponseDTO;
import com.vaPaTi.vaPaTi.entity.donation.DonationCtaText;
import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DonationSettingsMapper Tests")
class DonationSettingsMapperTest {

    private final DonationSettingsMapper mapper = new DonationSettingsMapper();

    @Test
    @DisplayName("Should map every field of a saved row, with the effective minimum and the suggested amounts in order")
    void toDTO_WithSavedRow_ShouldMapAllFields() {
        // Given
        DonationSettings settings = DonationSettings.builder()
                .mode(DonationMode.UNIT)
                .unitName("café")
                .unitAmount(new BigDecimal("4.00"))
                .minimumAmount(new BigDecimal("3.00"))
                .ctaText(DonationCtaText.TIP)
                .build();
        settings.replaceSuggestedAmounts(List.of(new BigDecimal("4.00"), new BigDecimal("8.00"), new BigDecimal("12.00")));

        // When
        DonationSettingsResponseDTO result = mapper.toDTO(20004L, settings);

        // Then
        assertThat(result.getUserId()).isEqualTo(20004L);
        assertThat(result.getMode()).isEqualTo(DonationMode.UNIT);
        assertThat(result.getUnitName()).isEqualTo("café");
        assertThat(result.getUnitAmount()).isEqualByComparingTo("4.00");
        assertThat(result.getMinimumAmount()).isEqualByComparingTo("3.00");
        assertThat(result.getEffectiveMinimumAmount()).isEqualByComparingTo("4.00");
        assertThat(result.getSuggestedAmounts()).containsExactly(
                new BigDecimal("4.00"), new BigDecimal("8.00"), new BigDecimal("12.00"));
        assertThat(result.getCtaText()).isEqualTo(DonationCtaText.TIP);
    }

    @Test
    @DisplayName("Should map the default settings with the given userId, SIMPLE, DONATE, null amounts and no suggested amounts")
    void toDTO_WithDefaultSettings_ShouldMapDefaults() {
        // Given
        DonationSettings defaults = DonationSettings.builder().build();

        // When
        DonationSettingsResponseDTO result = mapper.toDTO(20004L, defaults);

        // Then
        assertThat(result.getUserId()).isEqualTo(20004L);
        assertThat(result.getMode()).isEqualTo(DonationMode.SIMPLE);
        assertThat(result.getCtaText()).isEqualTo(DonationCtaText.DONATE);
        assertThat(result.getUnitName()).isNull();
        assertThat(result.getUnitAmount()).isNull();
        assertThat(result.getMinimumAmount()).isNull();
        assertThat(result.getEffectiveMinimumAmount()).isNull();
        assertThat(result.getSuggestedAmounts()).isEmpty();
    }
}
