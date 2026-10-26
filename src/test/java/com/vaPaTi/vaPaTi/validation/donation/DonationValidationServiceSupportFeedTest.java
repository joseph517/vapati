package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationValidationService - Support Feed Tests")
class DonationValidationServiceSupportFeedTest {

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

    @Mock
    private UserValidationService userValidationService;

    @InjectMocks
    private DonationValidationService donationValidationService;

    @Nested
    @DisplayName("isCampaignPublic")
    class IsCampaignPublic {

        @Test
        @DisplayName("Should not be public when the campaign was deleted, without asking the campaign domain")
        void isCampaignPublic_DeletedCampaign_ShouldReturnFalse() {
            // Given: a soft-deleted campaign keeps campaignId but leaves the relation null
            Donation donation = Donation.builder().id(1L).campaignId(10L).campaign(null).build();

            // When
            boolean result = donationValidationService.isCampaignPublic(donation);

            // Then
            assertThat(result).isFalse();
            verifyNoInteractions(campaignValidationService);
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @DisplayName("Should delegate to the campaign domain and return its result")
        void isCampaignPublic_CampaignLoaded_ShouldDelegate(boolean isPublic) {
            // Given
            Campaign campaign = Campaign.builder().id(10L).build();
            Donation donation = Donation.builder().id(1L).campaignId(10L).campaign(campaign).build();
            when(campaignValidationService.isPublicCampaign(campaign)).thenReturn(isPublic);

            // When
            boolean result = donationValidationService.isCampaignPublic(donation);

            // Then
            assertThat(result).isEqualTo(isPublic);
            verify(campaignValidationService).isPublicCampaign(campaign);
        }
    }
}
