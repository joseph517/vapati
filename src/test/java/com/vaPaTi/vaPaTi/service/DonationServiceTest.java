package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignStatisticsDTO;
import com.vaPaTi.vaPaTi.dtos.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Donation;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.DonationMapper;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.DonationRepository;
import com.vaPaTi.vaPaTi.repository.GoalRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.DonationStatus;
import com.vaPaTi.vaPaTi.validation.DonationValidationService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationService Tests")
class DonationServiceTest {

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

    private static final Long TEST_DONOR_ID = 1L;
    private static final Long TEST_CAMPAIGN_ID = 2L;
    private static final Long TEST_CAMPAIGN_OWNER_ID = 3L;
    private static final BigDecimal TEST_AMOUNT = new BigDecimal("100.0");
    private static final BigDecimal INITIAL_AMOUNT_RAISED = new BigDecimal("500.0");
    private static final BigDecimal GOAL_AMOUNT = new BigDecimal("1000.0");
    private static final String TEST_CAMPAIGN_NAME = "Test Campaign";

    private User testDonor;
    private User testCampaignOwner;
    private Campaign testCampaign;
    private Goal testGoal;
    private CreateDonationDTO createDonationDTO;
    private Donation testDonation;
    private DonationResponseDTO donationResponseDTO;

    @BeforeEach
    void setUp() {
        testDonor = new User();
        testDonor.setId(TEST_DONOR_ID);

        testCampaignOwner = new User();
        testCampaignOwner.setId(TEST_CAMPAIGN_OWNER_ID);

        testGoal = Goal.builder()
                .id(1L)
                .amountGoal(GOAL_AMOUNT)
                .amountRaised(INITIAL_AMOUNT_RAISED)
                .status(CampaignStatus.ACTIVE)
                .build();

        testCampaign = Campaign.builder()
                .id(TEST_CAMPAIGN_ID)
                .name(TEST_CAMPAIGN_NAME)
                .user(testCampaignOwner)
                .goal(testGoal)
                .build();

        createDonationDTO = new CreateDonationDTO();
        createDonationDTO.setCampaignId(TEST_CAMPAIGN_ID);
        createDonationDTO.setAmount(TEST_AMOUNT);

        testDonation = Donation.builder()
                .id(1L)
                .donor(testDonor)
                .campaign(testCampaign)
                .amount(TEST_AMOUNT)
                .status(DonationStatus.COMPLETED)
                .transactionId("TXN-12345678")
                .build();

        donationResponseDTO = DonationResponseDTO.builder()
                .id(1L)
                .donorUserId(TEST_DONOR_ID)
                .campaignId(TEST_CAMPAIGN_ID)
                .amount(TEST_AMOUNT)
                .status(DonationStatus.COMPLETED)
                .transactionId("TXN-12345678")
                .build();
    }

    @Nested
    @DisplayName("createDonation() tests")
    class CreateDonationTests {

        private void givenAmountAdded() {
            when(goalRepository.addToAmountRaised(eq(testGoal.getId()), eq(TEST_AMOUNT), any(LocalDateTime.class))).thenReturn(1);
        }

        @Test
        @DisplayName("Should create donation successfully with valid data")
        void createDonation_WithValidData_ShouldCreateDonation() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            givenAmountAdded();
            when(donationMapper.toDTO(testDonation)).thenReturn(donationResponseDTO);

            // When
            DonationResponseDTO result = donationService.createDonation(createDonationDTO);

            // Then
            assertThat(result).isNotNull().isEqualTo(donationResponseDTO);
            verify(authenticatedUserService).getAuthenticatedUserId();
            verify(donationValidationService).validateInput(createDonationDTO);
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID);
            verify(donationValidationService).validateGoalIsActive(testGoal);
            verify(donationValidationService).validateAndGetDonor(TEST_DONOR_ID);
            verify(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            verify(donationRepository).save(any(Donation.class));
            verify(donationMapper).toDTO(testDonation);
        }

        @Test
        @DisplayName("Should auto-approve donation with COMPLETED status")
        void createDonation_ShouldAutoApproveWithCompletedStatus() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            givenAmountAdded();
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            ArgumentCaptor<Donation> donationCaptor = ArgumentCaptor.forClass(Donation.class);

            // When
            donationService.createDonation(createDonationDTO);

            // Then
            verify(donationRepository).save(donationCaptor.capture());
            Donation savedDonation = donationCaptor.getValue();
            assertThat(savedDonation.getStatus()).isEqualTo(DonationStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should generate transaction ID for donation")
        void createDonation_ShouldGenerateTransactionId() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            givenAmountAdded();
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            ArgumentCaptor<Donation> donationCaptor = ArgumentCaptor.forClass(Donation.class);

            // When
            donationService.createDonation(createDonationDTO);

            // Then
            verify(donationRepository).save(donationCaptor.capture());
            Donation savedDonation = donationCaptor.getValue();
            assertThat(savedDonation.getTransactionId()).isNotNull();
            assertThat(savedDonation.getTransactionId()).startsWith("TXN-");
        }

        @Test
        @DisplayName("Should save the donation before adding its amount to the goal with an atomic update")
        void createDonation_ShouldSaveDonationBeforeAddingToAmountRaised() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            givenAmountAdded();
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            // When
            donationService.createDonation(createDonationDTO);

            // Then
            InOrder inOrder = inOrder(donationRepository, goalRepository);
            inOrder.verify(donationRepository).save(any(Donation.class));
            inOrder.verify(goalRepository).addToAmountRaised(eq(testGoal.getId()), eq(TEST_AMOUNT), any(LocalDateTime.class));
            inOrder.verify(goalRepository).completeIfGoalReached(eq(testGoal.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should throw MessageException and not complete the goal when the atomic add affects no row (goal closed meanwhile)")
        void createDonation_WhenAddToAmountRaisedAffectsNoRow_ShouldThrow() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            when(goalRepository.addToAmountRaised(eq(testGoal.getId()), eq(TEST_AMOUNT), any(LocalDateTime.class))).thenReturn(0);

            // When & Then
            assertThatThrownBy(() -> donationService.createDonation(createDonationDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign goal is not active");

            verify(goalRepository, never()).completeIfGoalReached(any(), any());
            verify(campaignStatusHistoryService, never()).recordTransition(any(), any(), any(), any());
            verify(donationMapper, never()).toDTO(any());
        }

        @Test
        @DisplayName("Should record an ACTIVE -> COMPLETED status history entry with no acting user when the conditional update completes the goal")
        void createDonation_WhenDonationReachesGoal_ShouldRecordStatusHistoryEntry() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            givenAmountAdded();
            when(goalRepository.completeIfGoalReached(eq(testGoal.getId()), any(LocalDateTime.class))).thenReturn(1);
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            // When
            donationService.createDonation(createDonationDTO);

            // Then
            verify(campaignStatusHistoryService)
                    .recordTransition(testCampaign, CampaignStatus.ACTIVE, CampaignStatus.COMPLETED, null);
        }

        @Test
        @DisplayName("Should not record a status history entry when the conditional update does not complete the goal")
        void createDonation_WhenDonationDoesNotReachGoal_ShouldNotRecordStatusHistoryEntry() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            givenAmountAdded();
            when(goalRepository.completeIfGoalReached(eq(testGoal.getId()), any(LocalDateTime.class))).thenReturn(0);
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            // When
            donationService.createDonation(createDonationDTO);

            // Then
            verify(campaignStatusHistoryService, never())
                    .recordTransition(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should not change amountRaised or status of the goal in memory")
        void createDonation_ShouldNotChangeGoalInMemory() {
            // Given
            testGoal.setAmountRaised(GOAL_AMOUNT.subtract(TEST_AMOUNT));
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            givenAmountAdded();
            when(goalRepository.completeIfGoalReached(eq(testGoal.getId()), any(LocalDateTime.class))).thenReturn(1);
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            // When
            donationService.createDonation(createDonationDTO);

            // Then
            assertThat(testGoal.getAmountRaised()).isEqualByComparingTo(GOAL_AMOUNT.subtract(TEST_AMOUNT));
            assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should accept donation when goal is already COMPLETED")
        void createDonation_WhenGoalIsCompleted_ShouldStillAcceptDonation() {
            // Given
            testGoal.setStatus(CampaignStatus.COMPLETED);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            givenAmountAdded();
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            // When
            DonationResponseDTO result = donationService.createDonation(createDonationDTO);

            // Then
            assertThat(result).isNotNull();
            verify(goalRepository).addToAmountRaised(eq(testGoal.getId()), eq(TEST_AMOUNT), any(LocalDateTime.class));
            verify(campaignStatusHistoryService, never()).recordTransition(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should validate input before creating donation")
        void createDonation_ShouldValidateInput() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            givenAmountAdded();
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            // When
            donationService.createDonation(createDonationDTO);

            // Then
            verify(donationValidationService).validateInput(createDonationDTO);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException before the status check when the campaign is not visible to the donor")
        void createDonation_WhenCampaignNotVisible_ShouldThrowNotFoundBeforeGoalStatusCheck() {
            // Given: a CLOSED campaign of someone else is not visible to the donor
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID))
                    .thenThrow(new ResourceNotFoundException("Campaign not found with id: " + TEST_CAMPAIGN_ID));

            // When & Then: 404 instead of the 400 "Campaign goal is not active"
            assertThatThrownBy(() -> donationService.createDonation(createDonationDTO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Campaign not found with id: " + TEST_CAMPAIGN_ID);

            verify(donationValidationService, never()).validateGoalIsActive(any());
            verify(donationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getDonationsByAuthenticatedUser() tests")
    class GetDonationsByAuthenticatedUserTests {

        @Test
        @DisplayName("Should return user donations ordered by createdAt DESC")
        void getDonationsByAuthenticatedUser_ShouldReturnUserDonations() {
            // Given
            List<Donation> donations = List.of(testDonation);
            List<DonationResponseDTO> expectedDTOs = List.of(donationResponseDTO);

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            when(donationRepository.findByDonorIdOrderByCreatedAtDesc(TEST_DONOR_ID)).thenReturn(donations);
            when(donationMapper.toDTOList(donations, Map.of())).thenReturn(expectedDTOs);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByAuthenticatedUser();

            // Then
            assertThat(result).isNotNull().hasSize(1).isEqualTo(expectedDTOs);
            verify(authenticatedUserService).getAuthenticatedUserId();
            verify(donationRepository).findByDonorIdOrderByCreatedAtDesc(TEST_DONOR_ID);
            verify(donationMapper).toDTOList(donations, Map.of());
        }

        @Test
        @DisplayName("Should return empty list when user has no donations")
        void getDonationsByAuthenticatedUser_WithNoDonations_ShouldReturnEmptyList() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            when(donationRepository.findByDonorIdOrderByCreatedAtDesc(TEST_DONOR_ID)).thenReturn(List.of());
            when(donationMapper.toDTOList(List.of(), Map.of())).thenReturn(List.of());

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByAuthenticatedUser();

            // Then
            assertThat(result).isNotNull().isEmpty();
            verify(donationRepository).findByDonorIdOrderByCreatedAtDesc(TEST_DONOR_ID);
        }

        @Test
        @DisplayName("Should retrieve authenticated user ID before querying donations")
        void getDonationsByAuthenticatedUser_ShouldGetAuthenticatedUserId() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            when(donationRepository.findByDonorIdOrderByCreatedAtDesc(TEST_DONOR_ID)).thenReturn(List.of());
            when(donationMapper.toDTOList(any(), any())).thenReturn(List.of());

            // When
            donationService.getDonationsByAuthenticatedUser();

            // Then
            verify(authenticatedUserService).getAuthenticatedUserId();
        }

        @Test
        @DisplayName("Should not query deleted campaign names when every campaign is alive")
        void getDonationsByAuthenticatedUser_WithAliveCampaigns_ShouldNotQueryDeletedNames() {
            // Given
            List<Donation> donations = List.of(testDonation);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            when(donationRepository.findByDonorIdOrderByCreatedAtDesc(TEST_DONOR_ID)).thenReturn(donations);
            when(donationMapper.toDTOList(donations, Map.of())).thenReturn(List.of(donationResponseDTO));

            // When
            donationService.getDonationsByAuthenticatedUser();

            // Then
            verifyNoInteractions(campaignRepository);
        }

        @Test
        @DisplayName("Should fetch the names of deleted campaigns in a single query and pass them to the mapper")
        void getDonationsByAuthenticatedUser_WithDeletedCampaigns_ShouldFetchNamesInSingleQuery() {
            // Given: two donations to deleted campaign 20 (relation null) and one to the alive campaign
            Donation toDeleted1 = Donation.builder().id(10L).donor(testDonor).campaign(null).campaignId(20L).build();
            Donation toDeleted2 = Donation.builder().id(11L).donor(testDonor).campaign(null).campaignId(20L).build();
            List<Donation> donations = List.of(testDonation, toDeleted1, toDeleted2);

            CampaignRepository.CampaignNameProjection projection = mock(CampaignRepository.CampaignNameProjection.class);
            when(projection.getId()).thenReturn(20L);
            when(projection.getName()).thenReturn("Deleted campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            when(donationRepository.findByDonorIdOrderByCreatedAtDesc(TEST_DONOR_ID)).thenReturn(donations);
            when(campaignRepository.findNamesByIdsIncludingDeleted(List.of(20L))).thenReturn(List.of(projection));
            when(donationMapper.toDTOList(donations, Map.of(20L, "Deleted campaign"))).thenReturn(List.of());

            // When
            donationService.getDonationsByAuthenticatedUser();

            // Then
            verify(campaignRepository, times(1)).findNamesByIdsIncludingDeleted(List.of(20L));
            verify(donationMapper).toDTOList(donations, Map.of(20L, "Deleted campaign"));
        }
    }

    @Nested
    @DisplayName("getDonationsByCampaign() tests")
    class GetDonationsByCampaignTests {

        @Test
        @DisplayName("Should return campaign donations when campaign exists")
        void getDonationsByCampaign_WithValidCampaignId_ShouldReturnDonations() {
            // Given
            List<Donation> donations = List.of(testDonation);
            List<DonationResponseDTO> expectedDTOs = List.of(donationResponseDTO);

            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_DONOR_ID));
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            when(donationRepository.findByCampaignOrderByCreatedAtDesc(testCampaign)).thenReturn(donations);
            when(donationMapper.toDTOList(donations)).thenReturn(expectedDTOs);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).isNotNull().hasSize(1).isEqualTo(expectedDTOs);
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID);
            verify(donationRepository).findByCampaignOrderByCreatedAtDesc(testCampaign);
            verify(donationMapper).toDTOList(donations);
        }

        @Test
        @DisplayName("Should return empty list when campaign has no donations")
        void getDonationsByCampaign_WithNoDonations_ShouldReturnEmptyList() {
            // Given
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_DONOR_ID));
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            when(donationRepository.findByCampaignOrderByCreatedAtDesc(testCampaign)).thenReturn(List.of());
            when(donationMapper.toDTOList(List.of())).thenReturn(List.of());

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).isNotNull().isEmpty();
            verify(donationRepository).findByCampaignOrderByCreatedAtDesc(testCampaign);
        }

        @Test
        @DisplayName("Should validate campaign exists before retrieving donations")
        void getDonationsByCampaign_ShouldValidateCampaign() {
            // Given
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_DONOR_ID));
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            when(donationRepository.findByCampaignOrderByCreatedAtDesc(testCampaign)).thenReturn(List.of());
            when(donationMapper.toDTOList(any())).thenReturn(List.of());

            // When
            donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID);
        }

        // A campaign with a donation of the test donor and one of another donor
        private static final Long OTHER_DONOR_ID = 4L;
        private static final Long THIRD_PARTY_ID = 5L;
        private static final Long ADMIN_ID = 6L;

        private List<DonationResponseDTO> givenDonationsFromTwoDonors(Long callerId) {
            donationResponseDTO.setDonorUserName("donor");
            DonationResponseDTO otherDonationDTO = DonationResponseDTO.builder()
                    .id(2L)
                    .donorUserId(OTHER_DONOR_ID)
                    .donorUserName("other")
                    .campaignId(TEST_CAMPAIGN_ID)
                    .amount(TEST_AMOUNT)
                    .status(DonationStatus.COMPLETED)
                    .transactionId("TXN-OTHER")
                    .build();
            List<Donation> donations = List.of(testDonation);
            List<DonationResponseDTO> dtos = List.of(donationResponseDTO, otherDonationDTO);

            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.ofNullable(callerId));
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, callerId)).thenReturn(testCampaign);
            when(donationRepository.findByCampaignOrderByCreatedAtDesc(testCampaign)).thenReturn(donations);
            when(donationMapper.toDTOList(donations)).thenReturn(dtos);
            return dtos;
        }

        private void assertDonorsAndAmountsVisible(List<DonationResponseDTO> result) {
            assertThat(result).extracting(DonationResponseDTO::getDonorUserId).containsExactly(TEST_DONOR_ID, OTHER_DONOR_ID);
            assertThat(result).extracting(DonationResponseDTO::getDonorUserName).containsExactly("donor", "other");
            assertThat(result).extracting(DonationResponseDTO::getAmount).doesNotContainNull();
        }

        @Test
        @DisplayName("Should look up the campaign with a null caller id and hide every transaction id for an anonymous caller")
        void getDonationsByCampaign_WhenAnonymous_ShouldHideTransactionId() {
            // Given
            givenDonationsFromTwoDonors(null);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then: transactionId is hidden, the donors stay visible
            assertThat(result).extracting(DonationResponseDTO::getTransactionId).containsOnlyNulls();
            assertDonorsAndAmountsVisible(result);
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID, null);
            verify(authenticatedUserService, never()).getAuthenticatedUserId();
            verifyNoInteractions(campaignAuthorizationService);
        }

        @Test
        @DisplayName("Should hide every transaction id for an authenticated third party")
        void getDonationsByCampaign_WhenThirdParty_ShouldHideTransactionIds() {
            // Given
            givenDonationsFromTwoDonors(THIRD_PARTY_ID);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).extracting(DonationResponseDTO::getTransactionId).containsOnlyNulls();
            assertDonorsAndAmountsVisible(result);
        }

        @Test
        @DisplayName("Should show a donor only the transaction id of their own donations")
        void getDonationsByCampaign_WhenDonor_ShouldShowOnlyOwnTransactionIds() {
            // Given
            givenDonationsFromTwoDonors(TEST_DONOR_ID);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).extracting(DonationResponseDTO::getTransactionId).containsExactly("TXN-12345678", null);
            assertDonorsAndAmountsVisible(result);
        }

        @Test
        @DisplayName("Should show the campaign owner every transaction id without checking the admin role")
        void getDonationsByCampaign_WhenOwner_ShouldShowAllTransactionIds() {
            // Given
            givenDonationsFromTwoDonors(TEST_CAMPAIGN_OWNER_ID);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).extracting(DonationResponseDTO::getTransactionId).containsExactly("TXN-12345678", "TXN-OTHER");
            assertDonorsAndAmountsVisible(result);
            verify(campaignAuthorizationService, never()).isAdmin(any());
        }

        @Test
        @DisplayName("Should show an ADMIN every transaction id")
        void getDonationsByCampaign_WhenAdmin_ShouldShowAllTransactionIds() {
            // Given
            givenDonationsFromTwoDonors(ADMIN_ID);
            when(campaignAuthorizationService.isAdmin(ADMIN_ID)).thenReturn(true);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).extracting(DonationResponseDTO::getTransactionId).containsExactly("TXN-12345678", "TXN-OTHER");
            assertDonorsAndAmountsVisible(result);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when the campaign is not visible to the caller")
        void getDonationsByCampaign_WhenCampaignNotVisible_ShouldThrowNotFound() {
            // Given
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_DONOR_ID));
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID))
                    .thenThrow(new ResourceNotFoundException("Campaign not found with id: " + TEST_CAMPAIGN_ID));

            // When & Then
            assertThatThrownBy(() -> donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(donationRepository, never()).findByCampaignOrderByCreatedAtDesc(any());
        }
    }

    @Nested
    @DisplayName("getCampaignStatistics() tests")
    class GetCampaignStatisticsTests {

        private void givenStatistics(Long totalDonations, Long uniqueDonors) {
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_DONOR_ID));
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            when(donationRepository.countCompletedDonationsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(totalDonations);
            when(donationRepository.countUniqueDonorsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(uniqueDonors);
        }

        @Test
        @DisplayName("Should calculate statistics correctly with valid data")
        void getCampaignStatistics_WithValidData_ShouldReturnStatistics() {
            // Given: 500 raised of 1000, in 4 donations from 3 donors
            givenStatistics(4L, 3L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCampaignId()).isEqualTo(TEST_CAMPAIGN_ID);
            assertThat(result.getCampaignName()).isEqualTo(TEST_CAMPAIGN_NAME);
            assertThat(result.getAmountGoal()).isEqualByComparingTo(GOAL_AMOUNT);
            assertThat(result.getAmountRaised()).isEqualByComparingTo(INITIAL_AMOUNT_RAISED);
            assertThat(result.getPercentageReached()).isEqualTo(new BigDecimal("50.00"));
            assertThat(result.getIsGoalReached()).isFalse();
            assertThat(result.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
            assertThat(result.getTotalDonors()).isEqualTo(3L);
            assertThat(result.getTotalDonations()).isEqualTo(4L);
            assertThat(result.getAverageDonation()).isEqualTo(new BigDecimal("125.00"));
        }

        @Test
        @DisplayName("Should return the goal's amountRaised even if it does not match the donations")
        void getCampaignStatistics_ShouldUseGoalAmountRaised() {
            // Given: the goal says 500 raised; 1 completed donation (of 100) is what the donations table has
            givenStatistics(1L, 1L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then: the same value the campaign returns
            assertThat(result.getAmountRaised()).isSameAs(testGoal.getAmountRaised());
            assertThat(result.getAverageDonation()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Should round the percentage and the average donation to 2 decimals HALF_UP")
        void getCampaignStatistics_ShouldRoundPercentageAndAverage() {
            // Given: 200 raised of 300, in 3 donations -> 66.666...% and 66.666... per donation
            testGoal.setAmountGoal(new BigDecimal("300.00"));
            testGoal.setAmountRaised(new BigDecimal("200.00"));
            givenStatistics(3L, 3L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getPercentageReached()).isEqualTo(new BigDecimal("66.67"));
            assertThat(result.getAverageDonation()).isEqualTo(new BigDecimal("66.67"));
        }

        @Test
        @DisplayName("Should return an average donation of 0 when there are no donations")
        void getCampaignStatistics_WithNoDonations_ShouldReturnZeroAverage() {
            // Given
            testGoal.setAmountRaised(BigDecimal.ZERO);
            givenStatistics(0L, 0L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getAmountRaised()).isZero();
            assertThat(result.getTotalDonations()).isZero();
            assertThat(result.getAverageDonation()).isZero();
            assertThat(result.getPercentageReached()).isZero();
        }

        @Test
        @DisplayName("Should default null counts from the repository to zero")
        void getCampaignStatistics_WithNullCounts_ShouldDefaultToZero() {
            // Given
            givenStatistics(null, null);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getTotalDonors()).isZero();
            assertThat(result.getTotalDonations()).isZero();
            assertThat(result.getAverageDonation()).isZero();
        }

        @Test
        @DisplayName("Should mark goal as reached when amount raised equals or exceeds goal")
        void getCampaignStatistics_WithGoalReached_ShouldMarkAsReached() {
            // Given
            testGoal.setAmountRaised(GOAL_AMOUNT);
            givenStatistics(5L, 5L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getIsGoalReached()).isTrue();
            assertThat(result.getPercentageReached()).isEqualTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should return 0% instead of dividing by zero when the goal is not positive (dev test data)")
        void getCampaignStatistics_WithNonPositiveGoal_ShouldReturnZeroPercentage() {
            // Given
            testGoal.setAmountGoal(new BigDecimal("-100.00"));
            givenStatistics(1L, 1L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getPercentageReached()).isZero();
        }

        @Test
        @DisplayName("Should handle campaign without goal")
        void getCampaignStatistics_WithNoGoal_ShouldHandleGracefully() {
            // Given
            testCampaign.setGoal(null);
            givenStatistics(2L, 2L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getAmountGoal()).isZero();
            assertThat(result.getAmountRaised()).isZero();
            assertThat(result.getPercentageReached()).isZero();
            assertThat(result.getStatus()).isNull();
        }

        @Test
        @DisplayName("Should validate campaign exists before calculating statistics")
        void getCampaignStatistics_ShouldValidateCampaign() {
            // Given
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_DONOR_ID));
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID)).thenReturn(testCampaign);
            when(donationRepository.countCompletedDonationsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(0L);
            when(donationRepository.countUniqueDonorsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(0L);

            // When
            donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID);
        }

        @Test
        @DisplayName("Should look up the campaign with a null caller id for an anonymous caller")
        void getCampaignStatistics_WhenAnonymous_ShouldPassNullCallerId() {
            // Given
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, null)).thenReturn(testCampaign);
            when(donationRepository.countCompletedDonationsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(0L);
            when(donationRepository.countUniqueDonorsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(0L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getCampaignId()).isEqualTo(TEST_CAMPAIGN_ID);
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID, null);
            verify(authenticatedUserService, never()).getAuthenticatedUserId();
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when the campaign is not visible to the caller")
        void getCampaignStatistics_WhenCampaignNotVisible_ShouldThrowNotFound() {
            // Given
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_DONOR_ID));
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID, TEST_DONOR_ID))
                    .thenThrow(new ResourceNotFoundException("Campaign not found with id: " + TEST_CAMPAIGN_ID));

            // When & Then
            assertThatThrownBy(() -> donationService.getCampaignStatistics(TEST_CAMPAIGN_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(donationRepository);
        }
    }
}
