package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationCreatedResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.donation.DonationStatus;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationService - Direct donation to a user")
class DonationServiceDirectTest {

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
    private static final Long RECIPIENT_ID = 2L;
    private static final BigDecimal AMOUNT = new BigDecimal("10.00");
    private static final String AUTO_THANKS = "Thanks a lot!";

    private User donor;
    private User recipient;
    private CreateDonationDTO dto;
    private DonationResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        donor = User.builder().id(DONOR_ID).build();
        recipient = User.builder().id(RECIPIENT_ID).build();
        recipient.setUserInfo(UserInfo.builder().id(20L).userName("recipient_user").build());

        dto = new CreateDonationDTO();
        dto.setRecipientUserId(RECIPIENT_ID);
        dto.setAmount(AMOUNT);

        responseDTO = DonationResponseDTO.builder().id(100L).recipientUserId(RECIPIENT_ID).build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(DONOR_ID);
    }

    private void givenValidDirectDonation() {
        when(donationValidationService.validateAndGetRecipient(RECIPIENT_ID)).thenReturn(recipient);
        when(donationValidationService.validateAndGetDonor(DONOR_ID)).thenReturn(donor);
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(donationMapper.toDTO(any(Donation.class))).thenReturn(responseDTO);
    }

    private Donation captureSavedDonation() {
        ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("Successful direct donation")
    class SuccessTests {

        @Test
        @DisplayName("Should save a COMPLETED donation to the recipient without a campaign, with a transaction id and the normalized message")
        void createDonation_Direct_ShouldSaveDonationWithRecipientAndNoCampaign() {
            // Given
            dto.setMessage("  hello  ");
            dto.setMessagePrivate(true);
            when(donationValidationService.validateAndNormalizeMessage("  hello  ")).thenReturn("hello");
            givenValidDirectDonation();

            // When
            DonationCreatedResponseDTO result = donationService.createDonation(dto);

            // Then
            Donation saved = captureSavedDonation();
            assertThat(saved.getRecipient()).isSameAs(recipient);
            assertThat(saved.getDonor()).isSameAs(donor);
            assertThat(saved.getCampaign()).isNull();
            assertThat(saved.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(saved.getStatus()).isEqualTo(DonationStatus.COMPLETED);
            assertThat(saved.getTransactionId()).startsWith("TXN-");
            assertThat(saved.getMessage()).isEqualTo("hello");
            assertThat(saved.isMessagePrivate()).isTrue();
            assertThat(result.getDonation()).isSameAs(responseDTO);
            verify(donationValidationService).validateInput(dto);
        }

        @Test
        @DisplayName("Should not touch any campaign, goal or status history")
        void createDonation_Direct_ShouldNotTouchCampaignOrGoal() {
            // Given
            givenValidDirectDonation();

            // When
            donationService.createDonation(dto);

            // Then
            verify(donationValidationService, never()).validateAndGetCampaign(any(), any());
            verify(donationValidationService, never()).validateGoalIsActive(any());
            verify(donationValidationService, never()).validateNotSelfDonation(anyLong(), anyLong());
            verifyNoInteractions(goalRepository, campaignStatusHistoryService, campaignRepository);
        }

        @Test
        @DisplayName("Should validate recipient, then donor, then self-donation")
        void createDonation_Direct_ShouldValidateInOrder() {
            // Given
            givenValidDirectDonation();

            // When
            donationService.createDonation(dto);

            // Then
            InOrder inOrder = inOrder(donationValidationService, donationRepository);
            inOrder.verify(donationValidationService).validateInput(dto);
            inOrder.verify(donationValidationService).validateAndGetRecipient(RECIPIENT_ID);
            inOrder.verify(donationValidationService).validateAndGetDonor(DONOR_ID);
            inOrder.verify(donationValidationService).validateNotSelfDirectDonation(DONOR_ID, RECIPIENT_ID);
            inOrder.verify(donationRepository).save(any(Donation.class));
        }
    }

    @Nested
    @DisplayName("autoThanksMessage")
    class AutoThanksTests {

        @Test
        @DisplayName("Should return the recipient's default thanks message")
        void createDonation_Direct_ShouldReturnRecipientAutoThanks() {
            // Given
            recipient.getUserInfo().setAutoThanksMessage(AUTO_THANKS);
            givenValidDirectDonation();

            // When
            DonationCreatedResponseDTO result = donationService.createDonation(dto);

            // Then
            assertThat(result.getAutoThanksMessage()).isEqualTo(AUTO_THANKS);
            assertThat(captureSavedDonation().getThanksMessage()).isNull();
        }

        @Test
        @DisplayName("Should return null when the recipient has no default thanks message")
        void createDonation_Direct_WhenRecipientHasNoAutoThanks_ShouldReturnNull() {
            // Given
            givenValidDirectDonation();

            // When
            DonationCreatedResponseDTO result = donationService.createDonation(dto);

            // Then
            assertThat(result.getAutoThanksMessage()).isNull();
        }

        @Test
        @DisplayName("Should return null when the recipient has no userInfo loaded")
        void createDonation_Direct_WhenRecipientWithoutUserInfo_ShouldReturnNull() {
            // Given
            recipient.setUserInfo(null);
            givenValidDirectDonation();

            // When
            DonationCreatedResponseDTO result = donationService.createDonation(dto);

            // Then
            assertThat(result.getAutoThanksMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("Failures")
    class FailureTests {

        @Test
        @DisplayName("Should not save when the recipient is not found, and not load the donor")
        void createDonation_Direct_WhenRecipientNotFound_ShouldNotSave() {
            // Given
            when(donationValidationService.validateAndGetRecipient(RECIPIENT_ID))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            // When & Then
            assertThatThrownBy(() -> donationService.createDonation(dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
            verify(donationValidationService, never()).validateAndGetDonor(any());
            verify(donationRepository, never()).save(any());
            verifyNoInteractions(goalRepository, campaignStatusHistoryService);
        }

        @Test
        @DisplayName("Should not save when the recipient is an admin")
        void createDonation_Direct_WhenRecipientIsAdmin_ShouldNotSave() {
            // Given
            when(donationValidationService.validateAndGetRecipient(RECIPIENT_ID))
                    .thenThrow(new MessageException("Cannot donate to an admin"));

            // When & Then
            assertThatThrownBy(() -> donationService.createDonation(dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot donate to an admin");
            verify(donationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not save a direct donation to oneself")
        void createDonation_Direct_WhenSelfDonation_ShouldNotSave() {
            // Given
            dto.setRecipientUserId(DONOR_ID);
            when(donationValidationService.validateAndGetRecipient(DONOR_ID)).thenReturn(donor);
            when(donationValidationService.validateAndGetDonor(DONOR_ID)).thenReturn(donor);
            doThrow(new MessageException("Cannot donate to yourself"))
                    .when(donationValidationService).validateNotSelfDirectDonation(DONOR_ID, DONOR_ID);

            // When & Then
            assertThatThrownBy(() -> donationService.createDonation(dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot donate to yourself");
            verify(donationRepository, never()).save(any());
            verifyNoInteractions(goalRepository, campaignStatusHistoryService);
        }

        @Test
        @DisplayName("Should not reach the recipient checks when the input is invalid")
        void createDonation_Direct_WhenInputInvalid_ShouldNotValidateRecipient() {
            // Given
            dto.setAmount(BigDecimal.ZERO);
            doThrow(new MessageException("Amount must be greater than zero"))
                    .when(donationValidationService).validateInput(dto);

            // When & Then
            assertThatThrownBy(() -> donationService.createDonation(dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Amount must be greater than zero");
            verify(donationValidationService, never()).validateAndGetRecipient(any());
            verify(donationRepository, never()).save(any());
        }
    }
}
