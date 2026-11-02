package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationSettingsResponseDTO;
import com.vaPaTi.vaPaTi.entity.donation.DonationCtaText;
import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationSettingsMapper;
import com.vaPaTi.vaPaTi.repository.donation.DonationSettingsRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationSettingsService - getDonationSettings() Tests")
class DonationSettingsServiceGetDonationSettingsTest {

    @Mock
    private DonationSettingsRepository donationSettingsRepository;
    @Spy
    private DonationSettingsMapper donationSettingsMapper = new DonationSettingsMapper();
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private RoleValidationService roleValidationService;

    @InjectMocks
    private DonationSettingsService donationSettingsService;

    private static final Long OWNER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long THIRD_PARTY_ID = 3L;

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
    @DisplayName("Should throw MessageException for userId 0 without querying anything")
    void getDonationSettings_WithZeroId_ShouldThrowMessageException() {
        assertThatThrownBy(() -> donationSettingsService.getDonationSettings(0L))
                .isInstanceOf(MessageException.class)
                .hasMessage("ID must be a positive number");

        verifyNoInteractions(userValidationService, authenticatedUserService, roleValidationService,
                donationSettingsRepository);
    }

    @Test
    @DisplayName("Should throw MessageException for a null userId without querying anything")
    void getDonationSettings_WithNullId_ShouldThrowMessageException() {
        assertThatThrownBy(() -> donationSettingsService.getDonationSettings(null))
                .isInstanceOf(MessageException.class)
                .hasMessage("ID must be a positive number");

        verifyNoInteractions(userValidationService, authenticatedUserService, roleValidationService,
                donationSettingsRepository);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when the user doesn't exist, without reading the settings")
    void getDonationSettings_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        when(userValidationService.getUserById(999L)).thenThrow(new ResourceNotFoundException("User not found"));

        // When & Then
        assertThatThrownBy(() -> donationSettingsService.getDonationSettings(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verifyNoInteractions(donationSettingsRepository);
    }

    @Test
    @DisplayName("An anonymous caller should receive the settings of an active user")
    void getDonationSettings_AsAnonymousOnActiveUser_ShouldReturnSettings() {
        // Given
        when(userValidationService.getUserById(OWNER_ID)).thenReturn(owner);
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(savedSettings));

        // When
        DonationSettingsResponseDTO result = donationSettingsService.getDonationSettings(OWNER_ID);

        // Then
        assertSavedSettings(result);
        verify(userValidationService).validateProfileNotSanctioned(owner);
    }

    @Test
    @DisplayName("An anonymous caller should receive the defaults of an active user without a row, without saving")
    void getDonationSettings_AsAnonymousWithoutRow_ShouldReturnDefaultsWithoutSaving() {
        // Given
        when(userValidationService.getUserById(OWNER_ID)).thenReturn(owner);
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

        // When
        DonationSettingsResponseDTO result = donationSettingsService.getDonationSettings(OWNER_ID);

        // Then
        assertDefaults(result);
        verify(donationSettingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("A third party should get a 404 for a sanctioned user, without reading the settings")
    void getDonationSettings_AsThirdPartyOnSanctionedUser_ShouldThrowResourceNotFoundException() {
        // Given
        when(userValidationService.getUserById(OWNER_ID)).thenReturn(owner);
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(THIRD_PARTY_ID));
        when(roleValidationService.isAdmin(THIRD_PARTY_ID)).thenReturn(false);
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userValidationService).validateProfileNotSanctioned(owner);

        // When & Then
        assertThatThrownBy(() -> donationSettingsService.getDonationSettings(OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(donationSettingsRepository, never()).findByUserId(anyLong());
    }

    @Test
    @DisplayName("An anonymous caller should get a 404 for a sanctioned user, without reading the settings")
    void getDonationSettings_AsAnonymousOnSanctionedUser_ShouldThrowResourceNotFoundException() {
        // Given
        when(userValidationService.getUserById(OWNER_ID)).thenReturn(owner);
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userValidationService).validateProfileNotSanctioned(owner);

        // When & Then
        assertThatThrownBy(() -> donationSettingsService.getDonationSettings(OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(donationSettingsRepository, never()).findByUserId(anyLong());
    }

    @Test
    @DisplayName("The user should receive their own settings without the sanction check")
    void getDonationSettings_AsOwner_ShouldSkipSanctionCheck() {
        // Given
        when(userValidationService.getUserById(OWNER_ID)).thenReturn(owner);
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(OWNER_ID));
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(savedSettings));

        // When
        DonationSettingsResponseDTO result = donationSettingsService.getDonationSettings(OWNER_ID);

        // Then
        assertSavedSettings(result);
        verify(userValidationService, never()).validateProfileNotSanctioned(any());
    }

    @Test
    @DisplayName("An ADMIN should receive the settings without the sanction check")
    void getDonationSettings_AsAdmin_ShouldSkipSanctionCheck() {
        // Given
        when(userValidationService.getUserById(OWNER_ID)).thenReturn(owner);
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(ADMIN_ID));
        when(roleValidationService.isAdmin(ADMIN_ID)).thenReturn(true);
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

        // When
        DonationSettingsResponseDTO result = donationSettingsService.getDonationSettings(OWNER_ID);

        // Then
        assertDefaults(result);
        verify(userValidationService, never()).validateProfileNotSanctioned(any());
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
