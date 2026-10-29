package com.vaPaTi.vaPaTi.mapper.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationSettingsResponseDTO;
import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import org.springframework.stereotype.Component;

@Component
public class DonationSettingsMapper {

    // userId comes apart because the default settings have no user. The effective minimum and the suggested amounts
    // come from the entity, the mapper calculates nothing
    public DonationSettingsResponseDTO toDTO(Long userId, DonationSettings settings) {
        return DonationSettingsResponseDTO.builder()
                .userId(userId)
                .mode(settings.getMode())
                .unitName(settings.getUnitName())
                .unitAmount(settings.getUnitAmount())
                .minimumAmount(settings.getMinimumAmount())
                .effectiveMinimumAmount(settings.getEffectiveMinimumAmount())
                .suggestedAmounts(settings.getSuggestedAmounts())
                .ctaText(settings.getCtaText())
                .build();
    }
}
