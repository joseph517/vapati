package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationSettingsResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.UpdateDonationSettingsDTO;
import com.vaPaTi.vaPaTi.entity.donation.DonationCtaText;
import com.vaPaTi.vaPaTi.entity.donation.DonationMode;
import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationSettingsMapper;
import com.vaPaTi.vaPaTi.repository.donation.DonationSettingsRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.donation.DonationSettingsValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationSettingsService - updateMyDonationSettings() Tests")
class DonationSettingsServiceUpdateTest {

    @Mock
    private DonationSettingsRepository donationSettingsRepository;
    @Spy
    private DonationSettingsMapper donationSettingsMapper = new DonationSettingsMapper();
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private DonationSettingsValidationService donationSettingsValidationService;

    @InjectMocks
    private DonationSettingsService donationSettingsService;

    private static final Long OWNER_ID = 1L;
    private static final Long SETTINGS_ID = 50L;

    private User owner;
    private UpdateDonationSettingsDTO dto;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(OWNER_ID);

        dto = new UpdateDonationSettingsDTO();
        dto.setMode(DonationMode.SIMPLE);
        dto.setMinimumAmount(new BigDecimal("3"));
        dto.setSuggestedAmounts(List.of(new BigDecimal("10"), new BigDecimal("5")));
        dto.setCtaText(DonationCtaText.TIP);
    }

    // Stands in for the real rules: writes on the entity what the DTO asked for, so the response can be checked
    private void applyRulesFromDto() {
        doAnswer(invocation -> {
            DonationSettings settings = invocation.getArgument(0);
            UpdateDonationSettingsDTO request = invocation.getArgument(1);
            settings.setMode(request.getMode());
            settings.setMinimumAmount(request.getMinimumAmount().setScale(2));
            settings.replaceSuggestedAmounts(request.getSuggestedAmounts().stream()
                    .map(a -> a.setScale(2)).sorted().toList());
            settings.setCtaText(request.getCtaText());
            return null;
        }).when(donationSettingsValidationService).validateAndApply(any(DonationSettings.class), any(UpdateDonationSettingsDTO.class));
    }

    private void givenCaller() {
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(userValidationService.getUserById(OWNER_ID)).thenReturn(owner);
    }

    @Test
    @DisplayName("Should throw MessageException when the DTO is null, without touching anything")
    void updateMyDonationSettings_WithNullDto_ShouldThrowMessageException() {
        assertThatThrownBy(() -> donationSettingsService.updateMyDonationSettings(null))
                .isInstanceOf(MessageException.class)
                .hasMessage("DTO cannot be null");

        verifyNoInteractions(authenticatedUserService, userValidationService,
                donationSettingsRepository, donationSettingsValidationService);
    }

    @Test
    @DisplayName("Without a row, should save a new one for the user of the token")
    void updateMyDonationSettings_WithoutRow_ShouldSaveNewRowForCaller() {
        // Given
        givenCaller();
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());
        applyRulesFromDto();
        when(donationSettingsRepository.save(any(DonationSettings.class))).thenAnswer(invocation -> {
            DonationSettings settings = invocation.getArgument(0);
            settings.setId(SETTINGS_ID);
            return settings;
        });

        // When
        DonationSettingsResponseDTO result = donationSettingsService.updateMyDonationSettings(dto);

        // Then
        ArgumentCaptor<DonationSettings> captor = ArgumentCaptor.forClass(DonationSettings.class);
        verify(donationSettingsRepository).save(captor.capture());
        DonationSettings saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getMode()).isEqualTo(DonationMode.SIMPLE);
        assertThat(saved.getCtaText()).isEqualTo(DonationCtaText.TIP);

        assertThat(result.getUserId()).isEqualTo(OWNER_ID);
        assertThat(result.getMode()).isEqualTo(DonationMode.SIMPLE);
        assertThat(result.getMinimumAmount()).isEqualTo(new BigDecimal("3.00"));
        assertThat(result.getEffectiveMinimumAmount()).isEqualTo(new BigDecimal("3.00"));
        assertThat(result.getSuggestedAmounts()).containsExactly(new BigDecimal("5.00"), new BigDecimal("10.00"));
        assertThat(result.getCtaText()).isEqualTo(DonationCtaText.TIP);
    }

    @Test
    @DisplayName("With a row, should save the same row with the new values")
    void updateMyDonationSettings_WithRow_ShouldSaveSameRow() {
        // Given
        DonationSettings existing = DonationSettings.builder()
                .id(SETTINGS_ID)
                .user(owner)
                .mode(DonationMode.UNIT)
                .unitName("café")
                .unitAmount(new BigDecimal("4.00"))
                .ctaText(DonationCtaText.DONATE)
                .build();
        givenCaller();
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(existing));
        applyRulesFromDto();
        when(donationSettingsRepository.save(any(DonationSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DonationSettingsResponseDTO result = donationSettingsService.updateMyDonationSettings(dto);

        // Then
        verify(donationSettingsValidationService).validateAndApply(existing, dto);
        ArgumentCaptor<DonationSettings> captor = ArgumentCaptor.forClass(DonationSettings.class);
        verify(donationSettingsRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getId()).isEqualTo(SETTINGS_ID);
        assertThat(captor.getValue().getUser()).isSameAs(owner);

        assertThat(result.getUserId()).isEqualTo(OWNER_ID);
        assertThat(result.getCtaText()).isEqualTo(DonationCtaText.TIP);
    }

    @Test
    @DisplayName("Should read the user, look up the row, apply the rules and save, in that order")
    void updateMyDonationSettings_ShouldFollowOrder() {
        // Given
        givenCaller();
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());
        applyRulesFromDto();
        when(donationSettingsRepository.save(any(DonationSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        donationSettingsService.updateMyDonationSettings(dto);

        // Then
        InOrder order = inOrder(authenticatedUserService, userValidationService,
                donationSettingsRepository, donationSettingsValidationService);
        order.verify(authenticatedUserService).getAuthenticatedUserId();
        order.verify(userValidationService).getUserById(OWNER_ID);
        order.verify(donationSettingsRepository).findByUserId(OWNER_ID);
        order.verify(donationSettingsValidationService).validateAndApply(any(DonationSettings.class), any(UpdateDonationSettingsDTO.class));
        order.verify(donationSettingsRepository).save(any(DonationSettings.class));
    }

    @Test
    @DisplayName("When a rule fails, should propagate the error and save nothing")
    void updateMyDonationSettings_WhenRulesFail_ShouldNotSave() {
        // Given
        givenCaller();
        when(donationSettingsRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());
        doThrow(new MessageException("Suggested amounts must be distinct"))
                .when(donationSettingsValidationService).validateAndApply(any(DonationSettings.class), any(UpdateDonationSettingsDTO.class));

        // When & Then
        assertThatThrownBy(() -> donationSettingsService.updateMyDonationSettings(dto))
                .isInstanceOf(MessageException.class)
                .hasMessage("Suggested amounts must be distinct");

        verify(donationSettingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate the 404 when the caller's user doesn't exist, without reading or saving the settings")
    void updateMyDonationSettings_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(userValidationService.getUserById(OWNER_ID)).thenThrow(new ResourceNotFoundException("User not found"));

        // When & Then
        assertThatThrownBy(() -> donationSettingsService.updateMyDonationSettings(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verifyNoInteractions(donationSettingsRepository, donationSettingsValidationService);
    }
}
