package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignStatisticsDTO;
import com.vaPaTi.vaPaTi.dtos.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Donation;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.mapper.DonationMapper;
import com.vaPaTi.vaPaTi.repository.DonationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.DonationStatus;
import com.vaPaTi.vaPaTi.validation.DonationValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private DonationService donationService;

    private static final Long TEST_DONOR_ID = 1L;
    private static final Long TEST_CAMPAIGN_ID = 2L;
    private static final Long TEST_CAMPAIGN_OWNER_ID = 3L;
    private static final Double TEST_AMOUNT = 100.0;
    private static final Double INITIAL_AMOUNT_RAISED = 500.0;
    private static final Double GOAL_AMOUNT = 1000.0;
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
                .active(true)
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

        @Test
        @DisplayName("Should create donation successfully with valid data")
        void createDonation_WithValidData_ShouldCreateDonation() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            when(donationMapper.toDTO(testDonation)).thenReturn(donationResponseDTO);

            // When
            DonationResponseDTO result = donationService.createDonation(createDonationDTO);

            // Then
            assertThat(result).isNotNull().isEqualTo(donationResponseDTO);
            verify(authenticatedUserService).getAuthenticatedUserId();
            verify(donationValidationService).validateInput(createDonationDTO);
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID);
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
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
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
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
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
        @DisplayName("Should update goal amount raised when creating donation")
        void createDonation_ShouldUpdateGoalAmountRaised() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            // When
            donationService.createDonation(createDonationDTO);

            // Then
            Double expectedAmountRaised = INITIAL_AMOUNT_RAISED + TEST_AMOUNT;
            assertThat(testGoal.getAmountRaised()).isEqualTo(expectedAmountRaised);
        }

        @Test
        @DisplayName("Should validate input before creating donation")
        void createDonation_ShouldValidateInput() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            doNothing().when(donationValidationService).validateInput(createDonationDTO);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            doNothing().when(donationValidationService).validateGoalIsActive(testGoal);
            when(donationValidationService.validateAndGetDonor(TEST_DONOR_ID)).thenReturn(testDonor);
            doNothing().when(donationValidationService).validateNotSelfDonation(TEST_DONOR_ID, TEST_CAMPAIGN_OWNER_ID);
            when(donationRepository.save(any(Donation.class))).thenReturn(testDonation);
            when(donationMapper.toDTO(any(Donation.class))).thenReturn(donationResponseDTO);

            // When
            donationService.createDonation(createDonationDTO);

            // Then
            verify(donationValidationService).validateInput(createDonationDTO);
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
            when(donationMapper.toDTOList(donations)).thenReturn(expectedDTOs);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByAuthenticatedUser();

            // Then
            assertThat(result).isNotNull().hasSize(1).isEqualTo(expectedDTOs);
            verify(authenticatedUserService).getAuthenticatedUserId();
            verify(donationRepository).findByDonorIdOrderByCreatedAtDesc(TEST_DONOR_ID);
            verify(donationMapper).toDTOList(donations);
        }

        @Test
        @DisplayName("Should return empty list when user has no donations")
        void getDonationsByAuthenticatedUser_WithNoDonations_ShouldReturnEmptyList() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_DONOR_ID);
            when(donationRepository.findByDonorIdOrderByCreatedAtDesc(TEST_DONOR_ID)).thenReturn(List.of());
            when(donationMapper.toDTOList(List.of())).thenReturn(List.of());

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
            when(donationMapper.toDTOList(any())).thenReturn(List.of());

            // When
            donationService.getDonationsByAuthenticatedUser();

            // Then
            verify(authenticatedUserService).getAuthenticatedUserId();
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

            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(donationRepository.findByCampaignOrderByCreatedAtDesc(testCampaign)).thenReturn(donations);
            when(donationMapper.toDTOList(donations)).thenReturn(expectedDTOs);

            // When
            List<DonationResponseDTO> result = donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).isNotNull().hasSize(1).isEqualTo(expectedDTOs);
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID);
            verify(donationRepository).findByCampaignOrderByCreatedAtDesc(testCampaign);
            verify(donationMapper).toDTOList(donations);
        }

        @Test
        @DisplayName("Should return empty list when campaign has no donations")
        void getDonationsByCampaign_WithNoDonations_ShouldReturnEmptyList() {
            // Given
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
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
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(donationRepository.findByCampaignOrderByCreatedAtDesc(testCampaign)).thenReturn(List.of());
            when(donationMapper.toDTOList(any())).thenReturn(List.of());

            // When
            donationService.getDonationsByCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID);
        }
    }

    @Nested
    @DisplayName("getCampaignStatistics() tests")
    class GetCampaignStatisticsTests {

        @Test
        @DisplayName("Should calculate statistics correctly with valid data")
        void getCampaignStatistics_WithValidData_ShouldReturnStatistics() {
            // Given
            Double totalRaised = 600.0;
            Long uniqueDonors = 5L;

            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(donationRepository.sumCompletedDonationsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(totalRaised);
            when(donationRepository.countUniqueDonorsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(uniqueDonors);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCampaignId()).isEqualTo(TEST_CAMPAIGN_ID);
            assertThat(result.getCampaignName()).isEqualTo(TEST_CAMPAIGN_NAME);
            assertThat(result.getAmountGoal()).isEqualTo(GOAL_AMOUNT);
            assertThat(result.getAmountRaised()).isEqualTo(totalRaised);
            assertThat(result.getPercentageReached()).isEqualTo(60.0);
            assertThat(result.getIsGoalReached()).isFalse();
            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getTotalDonors()).isEqualTo(uniqueDonors);
        }

        @Test
        @DisplayName("Should handle null total raised from repository")
        void getCampaignStatistics_WithNullTotalRaised_ShouldDefaultToZero() {
            // Given
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(donationRepository.sumCompletedDonationsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(null);
            when(donationRepository.countUniqueDonorsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(0L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getAmountRaised()).isZero();
            assertThat(result.getPercentageReached()).isZero();
        }

        @Test
        @DisplayName("Should handle null unique donors from repository")
        void getCampaignStatistics_WithNullUniqueDonors_ShouldDefaultToZero() {
            // Given
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(donationRepository.sumCompletedDonationsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(0.0);
            when(donationRepository.countUniqueDonorsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(null);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getTotalDonors()).isZero();
        }

        @Test
        @DisplayName("Should mark goal as reached when total raised equals or exceeds goal")
        void getCampaignStatistics_WithGoalReached_ShouldMarkAsReached() {
            // Given
            Double totalRaised = GOAL_AMOUNT;
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(donationRepository.sumCompletedDonationsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(totalRaised);
            when(donationRepository.countUniqueDonorsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(5L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getIsGoalReached()).isTrue();
            assertThat(result.getPercentageReached()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should handle campaign without goal")
        void getCampaignStatistics_WithNoGoal_ShouldHandleGracefully() {
            // Given
            testCampaign.setGoal(null);
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(donationRepository.sumCompletedDonationsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(100.0);
            when(donationRepository.countUniqueDonorsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(2L);

            // When
            CampaignStatisticsDTO result = donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result.getAmountGoal()).isZero();
            assertThat(result.getPercentageReached()).isZero();
            assertThat(result.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should validate campaign exists before calculating statistics")
        void getCampaignStatistics_ShouldValidateCampaign() {
            // Given
            when(donationValidationService.validateAndGetCampaign(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(donationRepository.sumCompletedDonationsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(0.0);
            when(donationRepository.countUniqueDonorsByCampaignId(TEST_CAMPAIGN_ID)).thenReturn(0L);

            // When
            donationService.getCampaignStatistics(TEST_CAMPAIGN_ID);

            // Then
            verify(donationValidationService).validateAndGetCampaign(TEST_CAMPAIGN_ID);
        }
    }
}
