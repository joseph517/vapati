package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.donation.CreateDonationThanksDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationCreatedResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.donation.DonationStatus;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
    private static final Long ADMIN_ID = 4L;
    private static final Long THIRD_PARTY_ID = 5L;
    private static final Long DONATION_ID = 10L;
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

    private Donation completedDonation() {
        return Donation.builder()
                .id(DONATION_ID)
                .donor(donor)
                .campaign(campaign)
                .amount(AMOUNT)
                .status(DonationStatus.COMPLETED)
                .build();
    }

    private CreateDonationThanksDTO thanksDTO(String thanksMessage, Boolean thanksPrivate) {
        CreateDonationThanksDTO dto = new CreateDonationThanksDTO();
        dto.setThanksMessage(thanksMessage);
        dto.setThanksPrivate(thanksPrivate);
        return dto;
    }

    private void givenSavedAndMapped() {
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(donationMapper.toDTO(any(Donation.class))).thenReturn(DonationResponseDTO.builder().id(DONATION_ID).build());
    }

    private Donation captureSavedDonation() {
        ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("thankDonation()")
    class ThankDonationTests {

        private Donation donation;

        @BeforeEach
        void setUp() {
            donation = completedDonation();
        }

        private void givenOwnerReplying(String rawMessage, String normalizedMessage) {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
            when(donationValidationService.validateAndNormalizeThanksMessage(rawMessage)).thenReturn(normalizedMessage);
            when(donationValidationService.validateAndGetDonation(DONATION_ID)).thenReturn(donation);
            when(donationValidationService.validateAndGetDonationCampaign(donation)).thenReturn(campaign);
        }

        @Test
        @DisplayName("Should save the normalized text, the private flag and thankedAt")
        void thankDonation_AsOwner_ShouldSaveThanks() {
            // Given
            givenOwnerReplying("  Gracias, B  ", "Gracias, B");
            givenSavedAndMapped();
            LocalDateTime before = LocalDateTime.now();

            // When
            DonationResponseDTO result = donationService.thankDonation(DONATION_ID, thanksDTO("  Gracias, B  ", true));

            // Then
            assertThat(result).isNotNull();
            Donation saved = captureSavedDonation();
            assertThat(saved.getThanksMessage()).isEqualTo("Gracias, B");
            assertThat(saved.isThanksPrivate()).isTrue();
            assertThat(saved.getThankedAt()).isAfterOrEqualTo(before);
            verify(donationValidationService).validateIsCampaignOwner(campaign, OWNER_ID);
            verify(donationValidationService).validateDonationCompleted(donation);
            verify(donationValidationService).validateHasNoThanks(donation);
        }

        @Test
        @DisplayName("Should save thanksPrivate false when it is null")
        void thankDonation_WithNullPrivate_ShouldSaveFalse() {
            // Given
            givenOwnerReplying("Gracias", "Gracias");
            givenSavedAndMapped();

            // When
            donationService.thankDonation(DONATION_ID, thanksDTO("Gracias", null));

            // Then
            assertThat(captureSavedDonation().isThanksPrivate()).isFalse();
        }

        @Test
        @DisplayName("Should reply to a donation that has no donor message")
        void thankDonation_WithoutDonorMessage_ShouldSaveThanks() {
            // Given
            givenOwnerReplying("Gracias", "Gracias");
            givenSavedAndMapped();

            // When
            donationService.thankDonation(DONATION_ID, thanksDTO("Gracias", false));

            // Then
            Donation saved = captureSavedDonation();
            assertThat(saved.getMessage()).isNull();
            assertThat(saved.getThanksMessage()).isEqualTo("Gracias");
        }

        @Test
        @DisplayName("Should throw and not save when the caller is not the owner")
        void thankDonation_AsThirdParty_ShouldThrowAndNotSave() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(THIRD_PARTY_ID);
            when(donationValidationService.validateAndNormalizeThanksMessage("Gracias")).thenReturn("Gracias");
            when(donationValidationService.validateAndGetDonation(DONATION_ID)).thenReturn(donation);
            when(donationValidationService.validateAndGetDonationCampaign(donation)).thenReturn(campaign);
            doThrow(new ForbiddenActionException("Only the campaign owner can reply to this donation"))
                    .when(donationValidationService).validateIsCampaignOwner(campaign, THIRD_PARTY_ID);

            // When & Then
            assertThatThrownBy(() -> donationService.thankDonation(DONATION_ID, thanksDTO("Gracias", false)))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the campaign owner can reply to this donation");
            verify(donationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw and not save when the caller is an ADMIN who does not own the campaign")
        void thankDonation_AsAdmin_ShouldThrowAndNotSave() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(ADMIN_ID);
            when(donationValidationService.validateAndNormalizeThanksMessage("Gracias")).thenReturn("Gracias");
            when(donationValidationService.validateAndGetDonation(DONATION_ID)).thenReturn(donation);
            when(donationValidationService.validateAndGetDonationCampaign(donation)).thenReturn(campaign);
            doThrow(new ForbiddenActionException("Only the campaign owner can reply to this donation"))
                    .when(donationValidationService).validateIsCampaignOwner(campaign, ADMIN_ID);

            // When & Then
            assertThatThrownBy(() -> donationService.thankDonation(DONATION_ID, thanksDTO("Gracias", false)))
                    .isInstanceOf(ForbiddenActionException.class);
            verify(donationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw and not save when the campaign is soft deleted")
        void thankDonation_WithDeletedCampaign_ShouldThrowAndNotSave() {
            // Given
            donation.setCampaign(null);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
            when(donationValidationService.validateAndNormalizeThanksMessage("Gracias")).thenReturn("Gracias");
            when(donationValidationService.validateAndGetDonation(DONATION_ID)).thenReturn(donation);
            when(donationValidationService.validateAndGetDonationCampaign(donation))
                    .thenThrow(new MessageException("Cannot reply to a donation of a deleted campaign"));

            // When & Then
            assertThatThrownBy(() -> donationService.thankDonation(DONATION_ID, thanksDTO("Gracias", false)))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot reply to a donation of a deleted campaign");
            verify(donationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw and not save when the donation already has a reply, keeping its text")
        void thankDonation_AlreadyThanked_ShouldThrowAndNotSave() {
            // Given
            donation.setThanksMessage("Primera respuesta");
            donation.setThankedAt(LocalDateTime.now());
            givenOwnerReplying("Otra", "Otra");
            doThrow(new ConflictException("Donation already has a thanks message"))
                    .when(donationValidationService).validateHasNoThanks(donation);

            // When & Then
            assertThatThrownBy(() -> donationService.thankDonation(DONATION_ID, thanksDTO("Otra", false)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Donation already has a thanks message");
            assertThat(donation.getThanksMessage()).isEqualTo("Primera respuesta");
            verify(donationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw and not save when the text is blank, before loading the donation")
        void thankDonation_WithBlankText_ShouldThrowAndNotSave() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
            when(donationValidationService.validateAndNormalizeThanksMessage("   "))
                    .thenThrow(new MessageException("Thanks message cannot be blank"));

            // When & Then
            assertThatThrownBy(() -> donationService.thankDonation(DONATION_ID, thanksDTO("   ", false)))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Thanks message cannot be blank");
            verify(donationValidationService, never()).validateAndGetDonation(any());
            verify(donationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteDonationThanks()")
    class DeleteDonationThanksTests {

        private Donation donation;

        @BeforeEach
        void setUp() {
            donation = completedDonation();
            donation.setThanksMessage("Gracias, B");
            donation.setThanksPrivate(true);
            donation.setThankedAt(LocalDateTime.now());
        }

        private void givenCaller(Long callerId) {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(callerId);
            when(donationValidationService.validateAndGetDonation(DONATION_ID)).thenReturn(donation);
            when(donationValidationService.validateAndGetDonationCampaign(donation)).thenReturn(campaign);
        }

        private void assertThanksCleared(Donation saved) {
            assertThat(saved.getThanksMessage()).isNull();
            assertThat(saved.isThanksPrivate()).isFalse();
            assertThat(saved.getThankedAt()).isNull();
        }

        @Test
        @DisplayName("Should clear the three fields when the owner deletes the reply")
        void deleteDonationThanks_AsOwner_ShouldClearThanks() {
            // Given
            givenCaller(OWNER_ID);
            givenSavedAndMapped();

            // When
            DonationResponseDTO result = donationService.deleteDonationThanks(DONATION_ID);

            // Then
            assertThat(result).isNotNull();
            assertThanksCleared(captureSavedDonation());
            verify(donationValidationService).validateIsCampaignOwnerOrAdmin(campaign, OWNER_ID);
            verify(donationValidationService).validateHasThanks(donation);
        }

        @Test
        @DisplayName("Should clear the three fields when an ADMIN deletes the reply")
        void deleteDonationThanks_AsAdmin_ShouldClearThanks() {
            // Given
            givenCaller(ADMIN_ID);
            givenSavedAndMapped();

            // When
            donationService.deleteDonationThanks(DONATION_ID);

            // Then
            assertThanksCleared(captureSavedDonation());
            verify(donationValidationService).validateIsCampaignOwnerOrAdmin(campaign, ADMIN_ID);
        }

        @Test
        @DisplayName("Should throw and not save when a third party deletes the reply")
        void deleteDonationThanks_AsThirdParty_ShouldThrowAndNotSave() {
            // Given
            givenCaller(THIRD_PARTY_ID);
            doThrow(new ForbiddenActionException("Only the campaign owner or an admin can delete this reply"))
                    .when(donationValidationService).validateIsCampaignOwnerOrAdmin(campaign, THIRD_PARTY_ID);

            // When & Then
            assertThatThrownBy(() -> donationService.deleteDonationThanks(DONATION_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("Only the campaign owner or an admin can delete this reply");
            assertThat(donation.getThanksMessage()).isEqualTo("Gracias, B");
            verify(donationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw and not save when the donation has no reply")
        void deleteDonationThanks_WithoutThanks_ShouldThrowAndNotSave() {
            // Given
            donation.setThanksMessage(null);
            donation.setThanksPrivate(false);
            donation.setThankedAt(null);
            givenCaller(OWNER_ID);
            doThrow(new ResourceNotFoundException("Donation has no thanks message"))
                    .when(donationValidationService).validateHasThanks(donation);

            // When & Then
            assertThatThrownBy(() -> donationService.deleteDonationThanks(DONATION_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Donation has no thanks message");
            verify(donationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw and not save when the campaign is soft deleted")
        void deleteDonationThanks_WithDeletedCampaign_ShouldThrowAndNotSave() {
            // Given
            donation.setCampaign(null);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
            when(donationValidationService.validateAndGetDonation(DONATION_ID)).thenReturn(donation);
            when(donationValidationService.validateAndGetDonationCampaign(donation))
                    .thenThrow(new MessageException("Cannot reply to a donation of a deleted campaign"));

            // When & Then
            assertThatThrownBy(() -> donationService.deleteDonationThanks(DONATION_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot reply to a donation of a deleted campaign");
            verify(donationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should let the owner reply again after deleting the reply")
        void deleteDonationThanks_ThenThankAgain_ShouldSaveNewThanks() {
            // Given: the conflict check behaves like the real one, reading the donation's current state
            givenCaller(OWNER_ID);
            givenSavedAndMapped();
            when(donationValidationService.validateAndNormalizeThanksMessage("Gracias otra vez")).thenReturn("Gracias otra vez");
            doAnswer(invocation -> {
                if (invocation.<Donation>getArgument(0).getThanksMessage() != null) {
                    throw new ConflictException("Donation already has a thanks message");
                }
                return null;
            }).when(donationValidationService).validateHasNoThanks(donation);

            // When
            donationService.deleteDonationThanks(DONATION_ID);
            donationService.thankDonation(DONATION_ID, thanksDTO("Gracias otra vez", false));

            // Then
            assertThat(donation.getThanksMessage()).isEqualTo("Gracias otra vez");
            assertThat(donation.isThanksPrivate()).isFalse();
            assertThat(donation.getThankedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Thanks visibility in the listings")
    class ThanksVisibilityTests {

        // The private reply is on the donor's donation, the public one on another donor's
        private static final Long OTHER_DONOR_ID = 6L;
        private static final LocalDateTime PRIVATE_THANKED_AT = LocalDateTime.of(2026, 10, 15, 10, 0);
        private static final LocalDateTime PUBLIC_THANKED_AT = LocalDateTime.of(2026, 10, 15, 11, 0);

        private List<DonationResponseDTO> donationDTOs() {
            return List.of(
                    donationDTO(20L, DONOR_ID, "Gracias en privado", true, PRIVATE_THANKED_AT),
                    donationDTO(21L, OTHER_DONOR_ID, "Gracias a todos", false, PUBLIC_THANKED_AT));
        }

        private DonationResponseDTO donationDTO(Long id, Long donorUserId, String thanksMessage,
                                                boolean thanksPrivate, LocalDateTime thankedAt) {
            return DonationResponseDTO.builder()
                    .id(id)
                    .donorUserId(donorUserId)
                    .campaignId(CAMPAIGN_ID)
                    .amount(AMOUNT)
                    .messagePrivate(false)
                    .thanksMessage(thanksMessage)
                    .thanksPrivate(thanksPrivate)
                    .thankedAt(thankedAt)
                    .build();
        }

        private List<DonationResponseDTO> listCampaignAs(Long callerId) {
            List<Donation> donations = List.of(Donation.builder().id(20L).build());
            List<DonationResponseDTO> dtos = donationDTOs();

            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.ofNullable(callerId));
            when(donationValidationService.validateAndGetCampaign(CAMPAIGN_ID, callerId)).thenReturn(campaign);
            when(donationRepository.findByCampaignOrderByCreatedAtDesc(campaign)).thenReturn(donations);
            when(donationMapper.toDTOList(donations)).thenReturn(dtos);

            return donationService.getDonationsByCampaign(CAMPAIGN_ID);
        }

        private void assertPrivateThanksVisible(List<DonationResponseDTO> result) {
            assertThat(result).extracting(DonationResponseDTO::getThanksMessage)
                    .containsExactly("Gracias en privado", "Gracias a todos");
            assertThat(result).extracting(DonationResponseDTO::getThanksPrivate)
                    .containsExactly(true, false);
            assertThat(result).extracting(DonationResponseDTO::getThankedAt)
                    .containsExactly(PRIVATE_THANKED_AT, PUBLIC_THANKED_AT);
        }

        private void assertPrivateThanksHidden(List<DonationResponseDTO> result) {
            assertThat(result).extracting(DonationResponseDTO::getThanksMessage)
                    .containsExactly(null, "Gracias a todos");
            assertThat(result).extracting(DonationResponseDTO::getThanksPrivate)
                    .containsExactly(false, false);
            assertThat(result).extracting(DonationResponseDTO::getThankedAt)
                    .containsExactly(null, PUBLIC_THANKED_AT);
        }

        @Test
        @DisplayName("Should show the campaign owner the private reply")
        void getDonationsByCampaign_WhenOwner_ShouldShowPrivateThanks() {
            // When
            List<DonationResponseDTO> result = listCampaignAs(OWNER_ID);

            // Then
            assertPrivateThanksVisible(result);
        }

        @Test
        @DisplayName("Should show an ADMIN the private reply")
        void getDonationsByCampaign_WhenAdmin_ShouldShowPrivateThanks() {
            // Given
            when(campaignAuthorizationService.isAdmin(ADMIN_ID)).thenReturn(true);

            // When
            List<DonationResponseDTO> result = listCampaignAs(ADMIN_ID);

            // Then
            assertPrivateThanksVisible(result);
        }

        @Test
        @DisplayName("Should show the donor the private reply to their own donation")
        void getDonationsByCampaign_WhenDonor_ShouldShowOwnPrivateThanks() {
            // When
            List<DonationResponseDTO> result = listCampaignAs(DONOR_ID);

            // Then
            assertPrivateThanksVisible(result);
        }

        @Test
        @DisplayName("Should show another USER the private reply as no reply, without thankedAt, and the public reply")
        void getDonationsByCampaign_WhenThirdParty_ShouldHidePrivateThanks() {
            // When
            List<DonationResponseDTO> result = listCampaignAs(THIRD_PARTY_ID);

            // Then
            assertPrivateThanksHidden(result);
        }

        @Test
        @DisplayName("Should show an anonymous caller the private reply as no reply, without thankedAt, and the public reply")
        void getDonationsByCampaign_WhenAnonymous_ShouldHidePrivateThanks() {
            // When
            List<DonationResponseDTO> result = listCampaignAs(null);

            // Then
            assertPrivateThanksHidden(result);
        }

        @Test
        @DisplayName("Should return the donor the private reply in my-donations")
        void getDonationsByAuthenticatedUser_ShouldShowPrivateThanks() {
            // Given
            List<Donation> donations = List.of(completedDonation());
            List<DonationResponseDTO> dtos = List.of(
                    donationDTO(DONATION_ID, DONOR_ID, "Gracias en privado", true, PRIVATE_THANKED_AT));
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(DONOR_ID);
            when(donationRepository.findByDonorIdOrderByCreatedAtDesc(DONOR_ID)).thenReturn(donations);
            when(donationMapper.toDTOList(donations, Map.of())).thenReturn(dtos);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByAuthenticatedUser();

            // Then
            assertThat(result).singleElement().satisfies(dto -> {
                assertThat(dto.getThanksMessage()).isEqualTo("Gracias en privado");
                assertThat(dto.getThanksPrivate()).isTrue();
                assertThat(dto.getThankedAt()).isEqualTo(PRIVATE_THANKED_AT);
            });
        }
    }
}
