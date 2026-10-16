package com.vaPaTi.vaPaTi.validation.donation;

import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.donation.DonationStatus;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationValidationService - Thanks Tests")
class DonationValidationServiceThanksTest {

    private static final Long OWNER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long THIRD_PARTY_ID = 3L;

    @Mock
    private CampaignValidationService campaignValidationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private CampaignAuthorizationService campaignAuthorizationService;

    // Real instance: the text rule lives in TextValidationService
    @Spy
    private TextValidationService textValidationService = new TextValidationService();

    @InjectMocks
    private DonationValidationService donationValidationService;

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        campaign = Campaign.builder()
                .id(10L)
                .user(User.builder().id(OWNER_ID).build())
                .build();
    }

    @Nested
    @DisplayName("validateAndNormalizeThanksMessage")
    class ValidateAndNormalizeThanksMessageTests {

        @Test
        @DisplayName("Should return the thanks message without leading and trailing spaces")
        void withSurroundingSpaces_ShouldReturnTrimmed() {
            // When
            String result = donationValidationService.validateAndNormalizeThanksMessage("  Gracias, B  ");

            // Then
            assertThat(result).isEqualTo("Gracias, B");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\n\t"})
        @DisplayName("Should throw when the thanks message is missing, empty or only whitespace")
        void whenBlank_ShouldThrow(String thanksMessage) {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndNormalizeThanksMessage(thanksMessage))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Thanks message cannot be blank");
        }

        @Test
        @DisplayName("Should throw when the thanks message has more than 280 characters")
        void with281Characters_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndNormalizeThanksMessage("a".repeat(281)))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Thanks message must be at most 280 characters");
        }
    }

    @Nested
    @DisplayName("validateAndGetDonation")
    class ValidateAndGetDonationTests {

        @Test
        @DisplayName("Should return the donation when it exists")
        void whenExists_ShouldReturnDonation() {
            // Given
            Donation donation = Donation.builder().id(5L).build();
            when(donationRepository.findById(5L)).thenReturn(Optional.of(donation));

            // When
            Donation result = donationValidationService.validateAndGetDonation(5L);

            // Then
            assertThat(result).isSameAs(donation);
        }

        @Test
        @DisplayName("Should throw when the donation does not exist")
        void whenMissing_ShouldThrow() {
            // Given
            when(donationRepository.findById(99L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndGetDonation(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Donation not found with id: 99");
        }
    }

    @Nested
    @DisplayName("validateAndGetDonationCampaign")
    class ValidateAndGetDonationCampaignTests {

        @Test
        @DisplayName("Should return the campaign when it is alive")
        void withAliveCampaign_ShouldReturnCampaign() {
            // Given
            Donation donation = Donation.builder().id(5L).campaign(campaign).build();

            // When
            Campaign result = donationValidationService.validateAndGetDonationCampaign(donation);

            // Then
            assertThat(result).isSameAs(campaign);
        }

        @Test
        @DisplayName("Should throw when the campaign is soft deleted (relation null)")
        void withDeletedCampaign_ShouldThrow() {
            // Given
            Donation donation = Donation.builder().id(5L).campaign(null).campaignId(10L).build();

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateAndGetDonationCampaign(donation))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot reply to a donation of a deleted campaign");
        }
    }

    @Nested
    @DisplayName("validateIsCampaignOwner")
    class ValidateIsCampaignOwnerTests {

        @Test
        @DisplayName("Should pass when the caller owns the campaign")
        void whenOwner_ShouldPass() {
            // When & Then
            assertThatCode(() -> donationValidationService.validateIsCampaignOwner(campaign, OWNER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when the caller does not own the campaign")
        void whenThirdParty_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsCampaignOwner(campaign, THIRD_PARTY_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the campaign owner can reply to this donation");
        }

        @Test
        @DisplayName("Should throw for an ADMIN who does not own the campaign, without checking the role")
        void whenAdmin_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsCampaignOwner(campaign, ADMIN_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the campaign owner can reply to this donation");
            verifyNoInteractions(campaignAuthorizationService);
        }
    }

    @Nested
    @DisplayName("validateIsCampaignOwnerOrAdmin")
    class ValidateIsCampaignOwnerOrAdminTests {

        @Test
        @DisplayName("Should pass when the caller owns the campaign, without checking the role")
        void whenOwner_ShouldPass() {
            // When & Then
            assertThatCode(() -> donationValidationService.validateIsCampaignOwnerOrAdmin(campaign, OWNER_ID))
                    .doesNotThrowAnyException();
            verifyNoInteractions(campaignAuthorizationService);
        }

        @Test
        @DisplayName("Should pass when the caller is an ADMIN")
        void whenAdmin_ShouldPass() {
            // Given
            when(campaignAuthorizationService.isAdmin(ADMIN_ID)).thenReturn(true);

            // When & Then
            assertThatCode(() -> donationValidationService.validateIsCampaignOwnerOrAdmin(campaign, ADMIN_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when the caller is neither the owner nor an ADMIN")
        void whenThirdParty_ShouldThrow() {
            // Given
            when(campaignAuthorizationService.isAdmin(THIRD_PARTY_ID)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsCampaignOwnerOrAdmin(campaign, THIRD_PARTY_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the campaign owner or an admin can delete this reply");
        }
    }

    @Nested
    @DisplayName("validateDonationCompleted")
    class ValidateDonationCompletedTests {

        @Test
        @DisplayName("Should pass for a COMPLETED donation")
        void whenCompleted_ShouldPass() {
            // Given
            Donation donation = Donation.builder().id(5L).status(DonationStatus.COMPLETED).build();

            // When & Then
            assertThatCode(() -> donationValidationService.validateDonationCompleted(donation))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = DonationStatus.class, names = "COMPLETED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("Should throw for any status other than COMPLETED")
        void whenNotCompleted_ShouldThrow(DonationStatus status) {
            // Given
            Donation donation = Donation.builder().id(5L).status(status).build();

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateDonationCompleted(donation))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Only completed donations can be replied to");
        }
    }

    @Nested
    @DisplayName("validateHasNoThanks")
    class ValidateHasNoThanksTests {

        @Test
        @DisplayName("Should pass when the donation has no thanks message")
        void withoutThanks_ShouldPass() {
            // Given
            Donation donation = Donation.builder().id(5L).build();

            // When & Then
            assertThatCode(() -> donationValidationService.validateHasNoThanks(donation))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when the donation already has a thanks message")
        void withThanks_ShouldThrow() {
            // Given
            Donation donation = Donation.builder().id(5L)
                    .thanksMessage("Gracias").thankedAt(LocalDateTime.now())
                    .build();

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateHasNoThanks(donation))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Donation already has a thanks message");
        }
    }

    @Nested
    @DisplayName("validateHasThanks")
    class ValidateHasThanksTests {

        @Test
        @DisplayName("Should pass when the donation has a thanks message")
        void withThanks_ShouldPass() {
            // Given
            Donation donation = Donation.builder().id(5L)
                    .thanksMessage("Gracias").thankedAt(LocalDateTime.now())
                    .build();

            // When & Then
            assertThatCode(() -> donationValidationService.validateHasThanks(donation))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when the donation has no thanks message")
        void withoutThanks_ShouldThrow() {
            // Given
            Donation donation = Donation.builder().id(5L).build();

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateHasThanks(donation))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Donation has no thanks message");
        }
    }
}
