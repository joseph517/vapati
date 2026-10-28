package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Public profile fields: the default thanks message sent to supporters
@Service
@RequiredArgsConstructor
public class UserProfileValidationService {

    private static final int MAX_AUTO_THANKS_LENGTH = 280;
    private final TextValidationService textValidationService;

    // The length check repeats the DTO's @Size, as validateAndNormalizeMessage does for donations
    public String validateAndNormalizeAutoThanksMessage(String autoThanksMessage) {
        return textValidationService.normalizeRequiredText(autoThanksMessage, "Auto thanks message", MAX_AUTO_THANKS_LENGTH);
    }
}
