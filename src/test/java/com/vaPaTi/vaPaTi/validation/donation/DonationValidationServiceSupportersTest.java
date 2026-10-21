package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationValidationService - Supporters Tests")
class DonationValidationServiceSupportersTest {

    @Mock
    private CampaignValidationService campaignValidationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TextValidationService textValidationService;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private CampaignAuthorizationService campaignAuthorizationService;

    @InjectMocks
    private DonationValidationService donationValidationService;

    @Nested
    @DisplayName("validateSupporterSort")
    class ValidateSupporterSort {

        @Test
        @DisplayName("Should throw when sorting by lastDonationAt without the full view")
        void validateSupporterSort_LastDonationAtWithoutFullView_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateSupporterSort("lastDonationAt", false))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Only the creator or an admin can sort supporters by lastDonationAt");
        }

        @Test
        @DisplayName("Should accept sorting by lastDonationAt with the full view")
        void validateSupporterSort_LastDonationAtWithFullView_ShouldNotThrow() {
            // When & Then
            assertThatCode(() -> donationValidationService.validateSupporterSort("lastDonationAt", true))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @DisplayName("Should accept sorting by totalAmount with or without the full view")
        void validateSupporterSort_TotalAmount_ShouldNotThrow(boolean fullView) {
            // When & Then
            assertThatCode(() -> donationValidationService.validateSupporterSort("totalAmount", fullView))
                    .doesNotThrowAnyException();
        }
    }
}
