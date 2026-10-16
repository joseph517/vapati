package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationCreatedResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationService - Thanks Tests")
class DonationServiceThanksTest {

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
    private static final String AUTO_THANKS = "¡Gracias por tu apoyo!";

    private User donor;
    private User owner;
    private Campaign campaign;
    private Goal goal;

    @BeforeEach
    void setUp() {
        donor = new User();
        donor.setId(DONOR_ID);

        owner = new User();
        owner.setId(OWNER_ID);
        owner.setUserInfo(UserInfo.builder().id(30L).userName("owner_user").build());

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
    @DisplayName("createDonation() with the owner's auto thanks message")
    class CreateDonationAutoThanksTests {

        private DonationResponseDTO mappedDonation;

        private DonationCreatedResponseDTO createDonation() {
            CreateDonationDTO dto = new CreateDonationDTO();
            dto.setCampaignId(CAMPAIGN_ID);
            dto.setAmount(AMOUNT);

            mappedDonation = DonationResponseDTO.builder().id(10L).build();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(DONOR_ID);
            when(donationValidationService.validateAndGetCampaign(CAMPAIGN_ID, DONOR_ID)).thenReturn(campaign);
            when(donationValidationService.validateAndGetDonor(DONOR_ID)).thenReturn(donor);
            when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(goalRepository.addToAmountRaised(eq(goal.getId()), eq(AMOUNT), any(LocalDateTime.class))).thenReturn(1);
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(mappedDonation);

            return donationService.createDonation(dto);
        }

        @Test
        @DisplayName("Should return the owner's auto thanks message next to the donation when it is set")
        void createDonation_WithAutoThanksSet_ShouldReturnIt() {
            // Given
            owner.getUserInfo().setAutoThanksMessage(AUTO_THANKS);

            // When
            DonationCreatedResponseDTO result = createDonation();

            // Then
            assertThat(result.getAutoThanksMessage()).isEqualTo(AUTO_THANKS);
            assertThat(result.getDonation()).isSameAs(mappedDonation);
        }

        @Test
        @DisplayName("Should return autoThanksMessage null when the owner has not set one")
        void createDonation_WithoutAutoThanks_ShouldReturnNull() {
            // When
            DonationCreatedResponseDTO result = createDonation();

            // Then
            assertThat(result.getAutoThanksMessage()).isNull();
            assertThat(result.getDonation()).isSameAs(mappedDonation);
        }

        @Test
        @DisplayName("Should return autoThanksMessage null when the owner has no userInfo loaded")
        void createDonation_WithOwnerWithoutUserInfo_ShouldReturnNull() {
            // Given
            owner.setUserInfo(null);

            // When
            DonationCreatedResponseDTO result = createDonation();

            // Then
            assertThat(result.getAutoThanksMessage()).isNull();
        }

        @Test
        @DisplayName("Should not copy the auto thanks message into the saved donation")
        void createDonation_WithAutoThanksSet_ShouldSaveDonationWithoutThanks() {
            // Given
            owner.getUserInfo().setAutoThanksMessage(AUTO_THANKS);

            // When
            createDonation();

            // Then
            ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
            verify(donationRepository).save(captor.capture());
            Donation saved = captor.getValue();
            assertThat(saved.getThanksMessage()).isNull();
            assertThat(saved.isThanksPrivate()).isFalse();
            assertThat(saved.getThankedAt()).isNull();
        }
    }
}
