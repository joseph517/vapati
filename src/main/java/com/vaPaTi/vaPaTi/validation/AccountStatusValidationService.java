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
        if (isBanned(user)) {
            throw new ForbiddenActionException("Your account has been banned. Reason: " + reasonOf(user));
        }

        if (isSuspended(user)) {
            throw new ForbiddenActionException("Your account is suspended until " + user.getSuspendedUntil() +
                    ". Reason: " + reasonOf(user));
        }
    }

    // Same condition as validateNotBlocked, without revealing which sanction applies
    public boolean isBlocked(User user) {
        return isBanned(user) || isSuspended(user);
    }

    private boolean isBanned(User user) {
        return Boolean.TRUE.equals(user.getBanned());
    }

    // An expired suspension (suspendedUntil in the past) doesn't block
    private boolean isSuspended(User user) {
        return user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(LocalDateTime.now());
    }

    private String reasonOf(User user) {
        return user.getBannedReason() != null ? user.getBannedReason() : DEFAULT_VIOLATION_REASON;
    }
}
