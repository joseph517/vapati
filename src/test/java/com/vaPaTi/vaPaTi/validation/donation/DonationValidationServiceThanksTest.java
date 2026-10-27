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
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
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
    private static final Long RECIPIENT_ID = 4L;
    private static final Long CAMPAIGN_ID = 10L;

    @Mock
    private CampaignValidationService campaignValidationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DonationRepository donationRepository;

    // Real instance: the text rule lives in TextValidationService
    @Spy
    private TextValidationService textValidationService = new TextValidationService();

    @Mock
    private RoleValidationService roleValidationService;

    @InjectMocks
    private DonationValidationService donationValidationService;

    // The recipient of a campaign donation is the campaign owner
    private Donation campaignDonation;
    // A direct donation has no campaign: relation and id are null
    private Donation directDonation;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(OWNER_ID).build();
        Campaign campaign = Campaign.builder().id(CAMPAIGN_ID).user(owner).build();
        campaignDonation = Donation.builder()
                .id(5L)
                .campaign(campaign)
                .campaignId(CAMPAIGN_ID)
                .recipient(owner)
                .recipientUserId(OWNER_ID)
                .build();
        directDonation = Donation.builder()
                .id(6L)
                .recipient(User.builder().id(RECIPIENT_ID).build())
                .recipientUserId(RECIPIENT_ID)
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
    @DisplayName("validateDonationCampaignNotDeleted")
    class ValidateDonationCampaignNotDeletedTests {

        @Test
        @DisplayName("Should pass when the campaign is alive")
        void withAliveCampaign_ShouldPass() {
            // When & Then
            assertThatCode(() -> donationValidationService.validateDonationCampaignNotDeleted(campaignDonation))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when the campaign is soft deleted (relation null, id kept)")
        void withDeletedCampaign_ShouldThrow() {
            // Given
            campaignDonation.setCampaign(null);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateDonationCampaignNotDeleted(campaignDonation))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot reply to a donation of a deleted campaign");
        }

        @Test
        @DisplayName("Should pass for a direct donation, which has no campaign")
        void withDirectDonation_ShouldPass() {
            // When & Then
            assertThatCode(() -> donationValidationService.validateDonationCampaignNotDeleted(directDonation))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateIsRecipient")
    class ValidateIsRecipientTests {

        @Test
        @DisplayName("Should pass when the caller owns the campaign of a campaign donation")
        void campaignDonation_WhenOwner_ShouldPass() {
            // When & Then
            assertThatCode(() -> donationValidationService.validateIsRecipient(campaignDonation, OWNER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass when the caller is the recipient of a direct donation")
        void directDonation_WhenRecipient_ShouldPass() {
            // When & Then
            assertThatCode(() -> donationValidationService.validateIsRecipient(directDonation, RECIPIENT_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw the campaign owner message when a third party replies to a campaign donation")
        void campaignDonation_WhenThirdParty_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsRecipient(campaignDonation, THIRD_PARTY_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the campaign owner can reply to this donation");
        }

        @Test
        @DisplayName("Should throw the campaign owner message for an ADMIN, without checking the role")
        void campaignDonation_WhenAdmin_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsRecipient(campaignDonation, ADMIN_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the campaign owner can reply to this donation");
            verifyNoInteractions(roleValidationService);
        }

        @Test
        @DisplayName("Should throw the recipient message when a third party replies to a direct donation")
        void directDonation_WhenThirdParty_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsRecipient(directDonation, THIRD_PARTY_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the recipient can reply to this donation");
        }

        @Test
        @DisplayName("Should throw the recipient message for an ADMIN on a direct donation, without checking the role")
        void directDonation_WhenAdmin_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsRecipient(directDonation, ADMIN_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the recipient can reply to this donation");
            verifyNoInteractions(roleValidationService);
        }

        @Test
        @DisplayName("Should throw when there is no caller id")
        void whenCallerIdNull_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsRecipient(directDonation, null))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the recipient can reply to this donation");
        }

        @Test
        @DisplayName("Should throw when the donation has no recipient id")
        void whenRecipientIdNull_ShouldThrow() {
            // Given
            directDonation.setRecipientUserId(null);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsRecipient(directDonation, RECIPIENT_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the recipient can reply to this donation");
        }
    }

    @Nested
    @DisplayName("validateIsRecipientOrAdmin")
    class ValidateIsRecipientOrAdminTests {

        @Test
        @DisplayName("Should pass when the caller owns the campaign of a campaign donation, without checking the role")
        void campaignDonation_WhenOwner_ShouldPass() {
            // When & Then
            assertThatCode(() -> donationValidationService.validateIsRecipientOrAdmin(campaignDonation, OWNER_ID))
                    .doesNotThrowAnyException();
            verifyNoInteractions(roleValidationService);
        }

        @Test
        @DisplayName("Should pass when the caller is the recipient of a direct donation, without checking the role")
        void directDonation_WhenRecipient_ShouldPass() {
            // When & Then
            assertThatCode(() -> donationValidationService.validateIsRecipientOrAdmin(directDonation, RECIPIENT_ID))
                    .doesNotThrowAnyException();
            verifyNoInteractions(roleValidationService);
        }

        @Test
        @DisplayName("Should pass when an ADMIN deletes the reply of a campaign donation")
        void campaignDonation_WhenAdmin_ShouldPass() {
            // Given
            when(roleValidationService.isAdmin(ADMIN_ID)).thenReturn(true);

            // When & Then
            assertThatCode(() -> donationValidationService.validateIsRecipientOrAdmin(campaignDonation, ADMIN_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass when an ADMIN deletes the reply of a direct donation")
        void directDonation_WhenAdmin_ShouldPass() {
            // Given
            when(roleValidationService.isAdmin(ADMIN_ID)).thenReturn(true);

            // When & Then
            assertThatCode(() -> donationValidationService.validateIsRecipientOrAdmin(directDonation, ADMIN_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw the campaign owner message when a third party deletes the reply of a campaign donation")
        void campaignDonation_WhenThirdParty_ShouldThrow() {
            // Given
            when(roleValidationService.isAdmin(THIRD_PARTY_ID)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsRecipientOrAdmin(campaignDonation, THIRD_PARTY_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the campaign owner or an admin can delete this reply");
        }

        @Test
        @DisplayName("Should throw the recipient message when a third party deletes the reply of a direct donation")
        void directDonation_WhenThirdParty_ShouldThrow() {
            // Given
            when(roleValidationService.isAdmin(THIRD_PARTY_ID)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> donationValidationService.validateIsRecipientOrAdmin(directDonation, THIRD_PARTY_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the recipient or an admin can delete this reply");
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
