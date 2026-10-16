package com.vaPaTi.vaPaTi.validation.common;

import com.vaPaTi.vaPaTi.exception.MessageException;
import org.springframework.stereotype.Service;

// Shared rule for required free-text fields; fieldLabel starts each error message (e.g. "Thanks message")
@Service
public class TextValidationService {

    public String normalizeRequiredText(String text, String fieldLabel, int maxLength) {
        if (text == null || text.trim().isEmpty()) {
            throw new MessageException(fieldLabel + " cannot be blank");
        }

        String trimmed = text.trim();
        if (trimmed.length() > maxLength) {
            throw new MessageException(fieldLabel + " must be at most " + maxLength + " characters");
        }

        return trimmed;
    }
}
