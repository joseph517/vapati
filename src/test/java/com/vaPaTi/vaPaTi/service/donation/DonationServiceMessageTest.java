package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
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
}
