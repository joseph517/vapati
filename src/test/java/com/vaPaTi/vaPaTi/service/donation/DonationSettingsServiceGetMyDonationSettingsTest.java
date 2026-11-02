package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationSettingsResponseDTO;
import com.vaPaTi.vaPaTi.entity.donation.DonationCtaText;
import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationSettingsMapper;
import com.vaPaTi.vaPaTi.repository.donation.DonationSettingsRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationSettingsService - getMyDonationSettings() Tests")
class DonationSettingsServiceGetMyDonationSettingsTest {

    @Mock
    private DonationSettingsRepository donationSettingsRepository;
    @Spy
    private DonationSettingsMapper donationSettingsMapper = new DonationSettingsMapper();
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private DonationSettingsService donationSettingsService;

    private static final Long OWNER_ID = 1L;

    private User owner;
    private DonationSettings savedSettings;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(OWNER_ID);

        savedSettings = DonationSettings.builder()
                .id(50L)
                .user(owner)
                .mode(DonationMode.UNIT)
                .unitName("café")
                .unitAmount(new BigDecimal("4.00"))
                .minimumAmount(new BigDecimal("3.00"))
                .ctaText(DonationCtaText.TIP)
                .build();
        savedSettings.replaceSuggestedAmounts(List.of(
                new BigDecimal("4.00"), new BigDecimal("8.00"), new BigDecimal("12.00")));
    }

    @Test
    @DisplayName("Should return the saved row of the caller")
    void getMyDonationSettings_WithRow_ShouldReturnSavedSettings() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(userValidationService.getUserById(OWNER_ID)).thenReturn(owner);
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(savedSettings));

        // When
        DonationSettingsResponseDTO result = donationSettingsService.getMyDonationSettings();

        // Then
        assertSavedSettings(result);
    }

    @Test
    @DisplayName("Without a row, should return the defaults without saving anything")
    void getMyDonationSettings_WithoutRow_ShouldReturnDefaultsWithoutSaving() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(userValidationService.getUserById(OWNER_ID)).thenReturn(owner);
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

        // When
        DonationSettingsResponseDTO result = donationSettingsService.getMyDonationSettings();

        // Then
        assertDefaults(result);
        verify(donationSettingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate the 404 when the caller's user doesn't exist, without reading the settings")
    void getMyDonationSettings_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(userValidationService.getUserById(OWNER_ID)).thenThrow(new ResourceNotFoundException("User not found"));

        // When & Then
        assertThatThrownBy(() -> donationSettingsService.getMyDonationSettings())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verifyNoInteractions(donationSettingsRepository);
    }

    private void assertSavedSettings(DonationSettingsResponseDTO result) {
        assertThat(result.getUserId()).isEqualTo(OWNER_ID);
        assertThat(result.getMode()).isEqualTo(DonationMode.UNIT);
        assertThat(result.getUnitName()).isEqualTo("café");
        assertThat(result.getUnitAmount()).isEqualByComparingTo("4.00");
        assertThat(result.getMinimumAmount()).isEqualByComparingTo("3.00");
        assertThat(result.getEffectiveMinimumAmount()).isEqualByComparingTo("4.00");
        assertThat(result.getSuggestedAmounts()).containsExactly(
                new BigDecimal("4.00"), new BigDecimal("8.00"), new BigDecimal("12.00"));
        assertThat(result.getCtaText()).isEqualTo(DonationCtaText.TIP);
    }

    private void assertDefaults(DonationSettingsResponseDTO result) {
        assertThat(result.getUserId()).isEqualTo(OWNER_ID);
        assertThat(result.getMode()).isEqualTo(DonationMode.SIMPLE);
        assertThat(result.getUnitName()).isNull();
        assertThat(result.getUnitAmount()).isNull();
        assertThat(result.getMinimumAmount()).isNull();
        assertThat(result.getEffectiveMinimumAmount()).isNull();
        assertThat(result.getSuggestedAmounts()).isEmpty();
        assertThat(result.getCtaText()).isEqualTo(DonationCtaText.DONATE);
    }
}
