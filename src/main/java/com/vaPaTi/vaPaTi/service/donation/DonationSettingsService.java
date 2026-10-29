package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationSettingsResponseDTO;
import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationSettingsMapper;
import com.vaPaTi.vaPaTi.repository.donation.DonationSettingsRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonationSettingsService {

    private final DonationSettingsRepository donationSettingsRepository;
    private final DonationSettingsMapper donationSettingsMapper;
    private final UserValidationService userValidationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final RoleValidationService roleValidationService;

    // The caller's own panel: their row, or the defaults if they never saved one
    public DonationSettingsResponseDTO getMyDonationSettings() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        User user = userValidationService.getUserById(userId);

        return toResponse(user.getId());
    }

    // Anyone's panel, also without a token. Same order as SupporterService.getSupporters: only the user and admins
    // see a banned or suspended user, the rest get a 404 before the settings are read
    public DonationSettingsResponseDTO getDonationSettings(Long userId) {
        if (userId == null || userId <= 0) {
            throw new MessageException("ID must be a positive number");
        }

        User user = userValidationService.getUserById(userId);

        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        if (!userId.equals(callerId) && !roleValidationService.isAdmin(callerId)) {
            userValidationService.validateProfileNotSanctioned(user);
        }

        return toResponse(user.getId());
    }

    // Reading never creates the row: without one, the defaults are mapped and nothing is saved
    private DonationSettingsResponseDTO toResponse(Long userId) {
        DonationSettings settings = donationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> DonationSettings.builder().build());
        return donationSettingsMapper.toDTO(userId, settings);
    }
}
