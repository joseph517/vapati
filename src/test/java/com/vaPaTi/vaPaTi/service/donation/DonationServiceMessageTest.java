package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationMapper;
import com.vaPaTi.vaPaTi.repository.campaign.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.campaign.GoalRepository;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.campaign.CampaignStatusHistoryService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.donation.DonationValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationService - Message Tests")
class DonationServiceMessageTest {

    @Mock
    private DonationRepository donationRepository;
    @Mock
    private DonationValidationService donationValidationService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private DonationMapper donationMapper;
    @Mock
    private CampaignStatusHistoryService campaignStatusHistoryService;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private CampaignAuthorizationService campaignAuthorizationService;

    @InjectMocks
    private DonationService donationService;

    private static final Long DONOR_ID = 1L;
    private static final Long CAMPAIGN_ID = 2L;
    private static final Long OWNER_ID = 3L;
    private static final BigDecimal AMOUNT = new BigDecimal("100.0");

    private User donor;
    private Campaign campaign;
    private Goal goal;

    @BeforeEach
    void setUp() {
        donor = new User();
        donor.setId(DONOR_ID);

        User owner = new User();
        owner.setId(OWNER_ID);

        goal = Goal.builder()
                .id(1L)
                .amountGoal(new BigDecimal("1000.0"))
                .amountRaised(new BigDecimal("500.0"))
                .status(CampaignStatus.ACTIVE)
                .build();

        campaign = Campaign.builder()
                .id(CAMPAIGN_ID)
                .name("Test Campaign")
                .user(owner)
                .goal(goal)
                .build();
    }

    @Nested
    @DisplayName("createDonation() with message")
    class CreateDonationWithMessageTests {

        private CreateDonationDTO dto(String message, Boolean messagePrivate) {
            CreateDonationDTO dto = new CreateDonationDTO();
            dto.setCampaignId(CAMPAIGN_ID);
            dto.setAmount(AMOUNT);
            dto.setMessage(message);
            dto.setMessagePrivate(messagePrivate);
            return dto;
        }

        private Donation createAndCaptureSavedDonation(CreateDonationDTO dto, String normalizedMessage) {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(DONOR_ID);
            when(donationValidationService.validateAndNormalizeMessage(dto.getMessage())).thenReturn(normalizedMessage);
            when(donationValidationService.validateAndGetCampaign(CAMPAIGN_ID, DONOR_ID)).thenReturn(campaign);
            when(donationValidationService.validateAndGetDonor(DONOR_ID)).thenReturn(donor);
            when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(goalRepository.addToAmountRaised(eq(goal.getId()), eq(AMOUNT), any(LocalDateTime.class))).thenReturn(1);

            donationService.createDonation(dto);

            ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
            verify(donationRepository).save(captor.capture());
            return captor.getValue();
        }

        @Test
        @DisplayName("Should save message null and messagePrivate false when there is no message")
        void createDonation_WithoutMessage_ShouldSaveNoMessage() {
            // When
            Donation saved = createAndCaptureSavedDonation(dto(null, null), null);

            // Then
            assertThat(saved.getMessage()).isNull();
            assertThat(saved.isMessagePrivate()).isFalse();
        }

        @Test
        @DisplayName("Should save the normalized text and messagePrivate false for a public message")
        void createDonation_WithPublicMessage_ShouldSaveNormalizedMessage() {
            // When
            Donation saved = createAndCaptureSavedDonation(dto("  Mucha suerte  ", false), "Mucha suerte");

            // Then
            assertThat(saved.getMessage()).isEqualTo("Mucha suerte");
            assertThat(saved.isMessagePrivate()).isFalse();
        }

        @Test
        @DisplayName("Should save messagePrivate false when the flag is null")
        void createDonation_WithMessageAndNullFlag_ShouldSavePublicMessage() {
            // When
            Donation saved = createAndCaptureSavedDonation(dto("Mucha suerte", null), "Mucha suerte");

            // Then
            assertThat(saved.getMessage()).isEqualTo("Mucha suerte");
            assertThat(saved.isMessagePrivate()).isFalse();
        }

        @Test
        @DisplayName("Should save the normalized text and messagePrivate true for a private message")
        void createDonation_WithPrivateMessage_ShouldSavePrivateMessage() {
            // When
            Donation saved = createAndCaptureSavedDonation(dto(" Solo para vos ", true), "Solo para vos");

            // Then
            assertThat(saved.getMessage()).isEqualTo("Solo para vos");
            assertThat(saved.isMessagePrivate()).isTrue();
        }

        @Test
        @DisplayName("Should save messagePrivate false when the flag is true but there is no message")
        void createDonation_WithPrivateFlagAndNoMessage_ShouldSaveNotPrivate() {
            // When
            Donation saved = createAndCaptureSavedDonation(dto(null, true), null);

            // Then
            assertThat(saved.getMessage()).isNull();
            assertThat(saved.isMessagePrivate()).isFalse();
        }

        @Test
        @DisplayName("Should throw and neither save the donation nor add to the goal when the message is blank")
        void createDonation_WithBlankMessage_ShouldThrowAndNotSave() {
            // Given
            CreateDonationDTO dto = dto("   ", true);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(DONOR_ID);
            when(donationValidationService.validateAndNormalizeMessage("   "))
                    .thenThrow(new MessageException("Message cannot be blank"));

            // When & Then
            assertThatThrownBy(() -> donationService.createDonation(dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Message cannot be blank");
            verify(donationRepository, never()).save(any(Donation.class));
            verify(goalRepository, never()).addToAmountRaised(anyLong(), any(BigDecimal.class), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("getDonationsByCampaign() message visibility")
    class GetDonationsByCampaignMessageTests {

        // The private donation is the donor's, the public one and the one without message are another donor's
        private static final Long OTHER_DONOR_ID = 4L;
        private static final Long THIRD_PARTY_ID = 5L;
        private static final Long ADMIN_ID = 6L;

        private List<DonationResponseDTO> listAs(Long callerId) {
            List<Donation> donations = List.of(Donation.builder().id(10L).build());
            List<DonationResponseDTO> dtos = List.of(
                    donationDTO(10L, DONOR_ID, "Solo para vos", true),
                    donationDTO(11L, OTHER_DONOR_ID, "Mucha suerte", false),
                    donationDTO(12L, OTHER_DONOR_ID, null, false));

            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.ofNullable(callerId));
            when(donationValidationService.validateAndGetCampaign(CAMPAIGN_ID, callerId)).thenReturn(campaign);
            when(donationRepository.findByCampaignOrderByCreatedAtDesc(campaign)).thenReturn(donations);
            when(donationMapper.toDTOList(donations)).thenReturn(dtos);

            return donationService.getDonationsByCampaign(CAMPAIGN_ID);
        }

        private DonationResponseDTO donationDTO(Long id, Long donorUserId, String message, boolean messagePrivate) {
            return DonationResponseDTO.builder()
                    .id(id)
                    .donorUserId(donorUserId)
                    .campaignId(CAMPAIGN_ID)
                    .amount(AMOUNT)
                    .transactionId("TXN-" + id)
                    .message(message)
                    .messagePrivate(messagePrivate)
                    .build();
        }

        private void assertPrivateMessageVisible(List<DonationResponseDTO> result) {
            assertThat(result).extracting(DonationResponseDTO::getMessage)
                    .containsExactly("Solo para vos", "Mucha suerte", null);
            assertThat(result).extracting(DonationResponseDTO::getMessagePrivate)
                    .containsExactly(true, false, false);
        }

        private void assertPrivateMessageHidden(List<DonationResponseDTO> result) {
            assertThat(result).extracting(DonationResponseDTO::getMessage)
                    .containsExactly(null, "Mucha suerte", null);
            assertThat(result).extracting(DonationResponseDTO::getMessagePrivate)
                    .containsExactly(false, false, false);
            assertThat(result).extracting(DonationResponseDTO::getDonorUserId)
                    .containsExactly(DONOR_ID, OTHER_DONOR_ID, OTHER_DONOR_ID);
        }

        @Test
        @DisplayName("Should show the campaign owner the private message")
        void getDonationsByCampaign_WhenOwner_ShouldShowPrivateMessage() {
            // When
            List<DonationResponseDTO> result = listAs(OWNER_ID);

            // Then
            assertPrivateMessageVisible(result);
        }

        @Test
        @DisplayName("Should show an ADMIN the private message")
        void getDonationsByCampaign_WhenAdmin_ShouldShowPrivateMessage() {
            // Given
            when(campaignAuthorizationService.isAdmin(ADMIN_ID)).thenReturn(true);

            // When
            List<DonationResponseDTO> result = listAs(ADMIN_ID);

            // Then
            assertPrivateMessageVisible(result);
        }

        @Test
        @DisplayName("Should show the donor the private message of their own donation")
        void getDonationsByCampaign_WhenDonor_ShouldShowOwnPrivateMessage() {
            // When
            List<DonationResponseDTO> result = listAs(DONOR_ID);

            // Then
            assertPrivateMessageVisible(result);
        }

        @Test
        @DisplayName("Should show another USER the private donation as one without message, and the public message")
        void getDonationsByCampaign_WhenThirdParty_ShouldHidePrivateMessage() {
            // When
            List<DonationResponseDTO> result = listAs(THIRD_PARTY_ID);

            // Then
            assertPrivateMessageHidden(result);
        }

        @Test
        @DisplayName("Should show an anonymous caller the private donation as one without message, and the public message")
        void getDonationsByCampaign_WhenAnonymous_ShouldHidePrivateMessage() {
            // When
            List<DonationResponseDTO> result = listAs(null);

            // Then
            assertPrivateMessageHidden(result);
        }
    }
}
