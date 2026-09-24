package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AccountStatusValidationService - Unit Tests")
class AccountStatusValidationServiceTest {

    private AccountStatusValidationService accountStatusValidationService;
    private User user;

    @BeforeEach
    void setUp() {
        accountStatusValidationService = new AccountStatusValidationService();
        user = User.builder()
                .id(1L)
                .build();
    }

    @Nested
    @DisplayName("validateNotBlocked()")
    class ValidateNotBlockedTests {

        @Test
        @DisplayName("Does not throw for a user that is neither banned nor suspended")
        void shouldPassForRegularUser() {
            user.setBanned(false);

            assertThatCode(() -> accountStatusValidationService.validateNotBlocked(user))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Does not throw when banned is null")
        void shouldPassWhenBannedIsNull() {
            user.setBanned(null);

            assertThatCode(() -> accountStatusValidationService.validateNotBlocked(user))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Banned user with reason: throws with the reason")
        void shouldThrowForBannedUserWithReason() {
            user.setBanned(true);
            user.setBannedReason("Spam");

            assertThatThrownBy(() -> accountStatusValidationService.validateNotBlocked(user))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Your account has been banned. Reason: Spam");
        }

        @Test
        @DisplayName("Banned user without reason: throws with the default reason")
        void shouldThrowForBannedUserWithoutReason() {
            user.setBanned(true);

            assertThatThrownBy(() -> accountStatusValidationService.validateNotBlocked(user))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Your account has been banned. Reason: Violation of terms");
        }

        @Test
        @DisplayName("Suspended user (suspendedUntil in the future): throws with date and reason")
        void shouldThrowForSuspendedUser() {
            LocalDateTime until = LocalDateTime.now().plusDays(3);
            user.setSuspendedUntil(until);
            user.setBannedReason("Abuse");

            assertThatThrownBy(() -> accountStatusValidationService.validateNotBlocked(user))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Your account is suspended until " + until + ". Reason: Abuse");
        }

        @Test
        @DisplayName("Suspended user without reason: throws with the default reason")
        void shouldThrowForSuspendedUserWithoutReason() {
            LocalDateTime until = LocalDateTime.now().plusDays(3);
            user.setSuspendedUntil(until);

            assertThatThrownBy(() -> accountStatusValidationService.validateNotBlocked(user))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Your account is suspended until " + until + ". Reason: Violation of terms");
        }

        @Test
        @DisplayName("Expired suspension (suspendedUntil in the past) does not block")
        void shouldPassForExpiredSuspension() {
            user.setSuspendedUntil(LocalDateTime.now().minusDays(1));

            assertThatCode(() -> accountStatusValidationService.validateNotBlocked(user))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Ban takes precedence over suspension")
        void shouldReportBanBeforeSuspension() {
            user.setBanned(true);
            user.setSuspendedUntil(LocalDateTime.now().plusDays(3));

            assertThatThrownBy(() -> accountStatusValidationService.validateNotBlocked(user))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessageStartingWith("Your account has been banned");
        }

    }

    @Nested
    @DisplayName("isBlocked()")
    class IsBlockedTests {

        @Test
        @DisplayName("Returns true for a banned user")
        void shouldReturnTrueForBannedUser() {
            user.setBanned(true);

            assertThat(accountStatusValidationService.isBlocked(user)).isTrue();
        }

        @Test
        @DisplayName("Returns true for an active suspension (suspendedUntil in the future)")
        void shouldReturnTrueForActiveSuspension() {
            user.setSuspendedUntil(LocalDateTime.now().plusDays(3));

            assertThat(accountStatusValidationService.isBlocked(user)).isTrue();
        }

        @Test
        @DisplayName("Returns false for an expired suspension (suspendedUntil in the past)")
        void shouldReturnFalseForExpiredSuspension() {
            user.setSuspendedUntil(LocalDateTime.now().minusDays(1));

            assertThat(accountStatusValidationService.isBlocked(user)).isFalse();
        }

        @Test
        @DisplayName("Returns false when banned is null")
        void shouldReturnFalseWhenBannedIsNull() {
            user.setBanned(null);

            assertThat(accountStatusValidationService.isBlocked(user)).isFalse();
        }

        @Test
        @DisplayName("Returns false for a user that is neither banned nor suspended")
        void shouldReturnFalseForRegularUser() {
            user.setBanned(false);

            assertThat(accountStatusValidationService.isBlocked(user)).isFalse();
        }
    }
}
