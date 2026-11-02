package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.entity.user.Role;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationValidationService - Direct donation recipient")
class DonationValidationServiceRecipientTest {

    @Mock
    private UserValidationService userValidationService;

    @InjectMocks
    private DonationValidationService donationValidationService;

    private static final Long DONOR_ID = 1L;
    private static final Long RECIPIENT_ID = 2L;

    private User recipient;

    @BeforeEach
    void setUp() {
        recipient = User.builder()
                .id(RECIPIENT_ID)
                .role(Role.builder().id(1L).name("USER").build())
                .build();
    }

    @Nested
    @DisplayName("validateAndGetRecipient()")
    class ValidateAndGetRecipientTests {

        @Test
        @DisplayName("Should return an active USER recipient after checking existence and sanctions in order")
        void validateAndGetRecipient_WhenActiveUser_ShouldReturnRecipient() {
            // Given
            when(userValidationService.getUserById(RECIPIENT_ID)).thenReturn(recipient);

            // When
            User result = donationValidationService.validateAndGetRecipient(RECIPIENT_ID);

            // Then
            assertThat(result).isSameAs(recipient);
            InOrder inOrder = inOrder(userValidationService);
            inOrder.verify(userValidationService).getUserById(RECIPIENT_ID);
            inOrder.verify(userValidationService).validateProfileNotSanctioned(recipient);
        }

        @Test
        @DisplayName("Should propagate User not found and skip the sanction check when the user is missing or deleted")
        void validateAndGetRecipient_WhenUserNotFound_ShouldPropagateAndSkipSanctionCheck() {
            // Given
            when(userValidationService.getUserById(RECIPIENT_ID)).thenThrow(new ResourceNotFoundException("User not found"));

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndGetRecipient(RECIPIENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
            verify(userValidationService, never()).validateProfileNotSanctioned(any());
        }

        @Test
        @DisplayName("Should propagate User not found from the sanction check before reading the role")
        void validateAndGetRecipient_WhenSanctioned_ShouldPropagateBeforeRoleCheck() {
            // Given
            User sanctionedRecipient = mock(User.class);
            when(userValidationService.getUserById(RECIPIENT_ID)).thenReturn(sanctionedRecipient);
            doThrow(new ResourceNotFoundException("User not found"))
                    .when(userValidationService).validateProfileNotSanctioned(sanctionedRecipient);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndGetRecipient(RECIPIENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
            verify(sanctionedRecipient, never()).getRole();
        }

        @Test
        @DisplayName("Should reject an ADMIN recipient")
        void validateAndGetRecipient_WhenAdmin_ShouldThrowMessageException() {
            // Given
            recipient.setRole(Role.builder().id(2L).name("ADMIN").build());
            when(userValidationService.getUserById(RECIPIENT_ID)).thenReturn(recipient);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndGetRecipient(RECIPIENT_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot donate to an admin");
            verify(userValidationService).validateProfileNotSanctioned(recipient);
        }
    }

    @Nested
    @DisplayName("validateNotSelfDirectDonation()")
    class ValidateNotSelfDirectDonationTests {

        @Test
        @DisplayName("Should reject a direct donation to oneself")
        void validateNotSelfDirectDonation_WhenSameId_ShouldThrowMessageException() {
            assertThatThrownBy(() -> donationValidationService.validateNotSelfDirectDonation(DONOR_ID, DONOR_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot donate to yourself");
        }

        @Test
        @DisplayName("Should accept a direct donation to another user")
        void validateNotSelfDirectDonation_WhenDifferentId_ShouldPass() {
            assertThatCode(() -> donationValidationService.validateNotSelfDirectDonation(DONOR_ID, RECIPIENT_ID))
                    .doesNotThrowAnyException();
        }
    }
}
