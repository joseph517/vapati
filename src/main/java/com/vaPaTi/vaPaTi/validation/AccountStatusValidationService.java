package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Shared ban/suspension check used by login, refresh and the JWT filter
@Service
public class AccountStatusValidationService {

    private static final String DEFAULT_VIOLATION_REASON = "Violation of terms";

    public void validateNotBlocked(User user) {
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new ForbiddenActionException("Your account has been banned. Reason: " + reasonOf(user));
        }

        // An expired suspension (suspendedUntil in the past) doesn't block
        if (user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(LocalDateTime.now())) {
            throw new ForbiddenActionException("Your account is suspended until " + user.getSuspendedUntil() +
                    ". Reason: " + reasonOf(user));
        }
    }

    private String reasonOf(User user) {
        return user.getBannedReason() != null ? user.getBannedReason() : DEFAULT_VIOLATION_REASON;
    }
}
