package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.mapper.CampaignMapper;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.CampaignServiceValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignService Tests")
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CampaignServiceValidation campaignServiceValidation;

    @InjectMocks
    private CampaignService campaignService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_CAMPAIGN_ID = 1L;
    private static final Double DEFAULT_AMOUNT_RAISED = 0.0;
    private static final String CAMPAIGN_NOT_FOUND_MESSAGE = "Campaign not found";
    private static final String USER_NOT_FOUND_MESSAGE = "User not found with id: ";

    private User testUser;
    private Campaign testCampaign;
    private CreateCampaignRequestDTO createCampaignDTO;
    private UpdateCampaignRequestDTO updateCampaignDTO;
    private CampaignResponseDTO campaignResponseDTO;
    private Goal testGoal;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(TEST_USER_ID);

        testGoal = new Goal();
        testGoal.setId(1L);

        testCampaign = new Campaign();
        testCampaign.setId(TEST_CAMPAIGN_ID);
        testCampaign.setUser(testUser);
        testCampaign.setName("Test Campaign");
        testCampaign.setDescription("Test Description");
        testCampaign.setGoal(testGoal);

        createCampaignDTO = new CreateCampaignRequestDTO();
        createCampaignDTO.setName("New Campaign");
        createCampaignDTO.setDescription("New Description");

        updateCampaignDTO = new UpdateCampaignRequestDTO();
        updateCampaignDTO.setName("Updated Campaign");
        updateCampaignDTO.setDescription("Updated Description");

        campaignResponseDTO = new CampaignResponseDTO();
        campaignResponseDTO.setId(TEST_CAMPAIGN_ID);
        campaignResponseDTO.setName("Test Campaign");
    }

    @Nested
    @DisplayName("getAllCampaigns() tests")
    class GetAllCampaignsTests {

        @Test
        @DisplayName("Should return list of mapped campaign DTOs when campaigns exist")
        void getAllCampaigns_WithCampaigns_ShouldReturnMappedDTOs() {
            // Given
            Campaign campaign1 = createTestCampaign(1L, "Campaign 1");
            Campaign campaign2 = createTestCampaign(2L, "Campaign 2");
            Campaign campaign3 = createTestCampaign(3L, "Campaign 3");
            List<Campaign> campaigns = List.of(campaign1, campaign2, campaign3);

            CampaignResponseDTO dto1 = createResponseDTO(1L, "Campaign 1");
            CampaignResponseDTO dto2 = createResponseDTO(2L, "Campaign 2");
            CampaignResponseDTO dto3 = createResponseDTO(3L, "Campaign 3");

            when(campaignRepository.findAll()).thenReturn(campaigns);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(campaign1)).thenReturn(dto1);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(campaign2)).thenReturn(dto2);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(campaign3)).thenReturn(dto3);

                // When
                List<CampaignResponseDTO> result = campaignService.getAllCampaigns();

                // Then
                assertThat(result)
                        .isNotNull()
                        .hasSize(3)
                        .containsExactly(dto1, dto2, dto3);

                verify(campaignRepository).findAll();
                mapperMock.verify(() -> CampaignMapper.toResponseDTO(campaign1));
                mapperMock.verify(() -> CampaignMapper.toResponseDTO(campaign2));
                mapperMock.verify(() -> CampaignMapper.toResponseDTO(campaign3));
            }
        }

        @Test
        @DisplayName("Should return empty list when no campaigns exist")
        void getAllCampaigns_WithNoCampaigns_ShouldReturnEmptyList() {
            // Given
            when(campaignRepository.findAll()).thenReturn(List.of());

            // When
            List<CampaignResponseDTO> result = campaignService.getAllCampaigns();

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEmpty();

            verify(campaignRepository).findAll();
        }

        @Test
        @DisplayName("Should call repository findAll exactly once")
        void getAllCampaigns_ShouldCallRepositoryOnce() {
            // Given
            when(campaignRepository.findAll()).thenReturn(List.of());

            // When
            campaignService.getAllCampaigns();

            // Then
            verify(campaignRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("getCampaignsByAuthenticatedUser() tests")
    class GetCampaignsByAuthenticatedUserTests {

        @Test
        @DisplayName("Should return user's campaigns when campaigns exist")
        void getCampaignsByAuthenticatedUser_WithUserCampaigns_ShouldReturnUserCampaigns() {
            // Given
            Campaign campaign1 = createTestCampaign(1L, "User Campaign 1");
            Campaign campaign2 = createTestCampaign(2L, "User Campaign 2");
            List<Campaign> userCampaigns = List.of(campaign1, campaign2);

            CampaignResponseDTO dto1 = createResponseDTO(1L, "User Campaign 1");
            CampaignResponseDTO dto2 = createResponseDTO(2L, "User Campaign 2");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignRepository.findByUserId(TEST_USER_ID)).thenReturn(userCampaigns);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(campaign1)).thenReturn(dto1);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(campaign2)).thenReturn(dto2);

                // When
                List<CampaignResponseDTO> result = campaignService.getCampaignsByAuthenticatedUser();

                // Then
                assertThat(result)
                        .isNotNull()
                        .hasSize(2)
                        .containsExactly(dto1, dto2);

                verify(authenticatedUserService).getAuthenticatedUserId();
                verify(campaignRepository).findByUserId(TEST_USER_ID);
            }
        }

        @Test
        @DisplayName("Should return empty list when user has no campaigns")
        void getCampaignsByAuthenticatedUser_WithNoCampaigns_ShouldReturnEmptyList() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of());

            // When
            List<CampaignResponseDTO> result = campaignService.getCampaignsByAuthenticatedUser();

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEmpty();

            verify(campaignRepository).findByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should retrieve authenticated user ID before querying campaigns")
        void getCampaignsByAuthenticatedUser_ShouldGetAuthenticatedUserId() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of());

            // When
            campaignService.getCampaignsByAuthenticatedUser();

            // Then
            verify(authenticatedUserService).getAuthenticatedUserId();
        }
    }

    @Nested
    @DisplayName("createCampaign() tests")
    class CreateCampaignTests {

        @Test
        @DisplayName("Should create campaign successfully with valid data")
        void createCampaign_WithValidDTO_ShouldCreateCampaign() {
            // Given
            createCampaignDTO.setAmountRaised(50.0);
            Campaign savedCampaign = createTestCampaign(TEST_CAMPAIGN_ID, "New Campaign");
            CampaignResponseDTO expectedDTO = createResponseDTO(TEST_CAMPAIGN_ID, "New Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toEntity(createCampaignDTO, testUser))
                        .thenReturn(savedCampaign);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(savedCampaign))
                        .thenReturn(expectedDTO);

                // When
                CampaignResponseDTO result = campaignService.createCampaign(createCampaignDTO);

                // Then
                assertThat(result)
                        .isNotNull()
                        .isEqualTo(expectedDTO);

                verify(authenticatedUserService).getAuthenticatedUserId();
                verify(userRepository).findById(TEST_USER_ID);
                verify(campaignRepository).save(any(Campaign.class));
                mapperMock.verify(() -> CampaignMapper.toEntity(createCampaignDTO, testUser));
                mapperMock.verify(() -> CampaignMapper.toResponseDTO(savedCampaign));
            }
        }

        @Test
        @DisplayName("Should set amountRaised to 0 when DTO value is null")
        void createCampaign_WithNullAmountRaised_ShouldDefaultToZero() {
            // Given
            createCampaignDTO.setAmountRaised(null);
            Campaign savedCampaign = createTestCampaign(TEST_CAMPAIGN_ID, "New Campaign");
            CampaignResponseDTO expectedDTO = createResponseDTO(TEST_CAMPAIGN_ID, "New Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toEntity(any(CreateCampaignRequestDTO.class), eq(testUser)))
                        .thenReturn(savedCampaign);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(savedCampaign))
                        .thenReturn(expectedDTO);

                // When
                campaignService.createCampaign(createCampaignDTO);

                // Then
                assertThat(createCampaignDTO.getAmountRaised()).isEqualTo(DEFAULT_AMOUNT_RAISED);
            }
        }

        @Test
        @DisplayName("Should preserve amountRaised value when DTO value is provided")
        void createCampaign_WithProvidedAmountRaised_ShouldKeepValue() {
            // Given
            Double providedAmount = 100.0;
            createCampaignDTO.setAmountRaised(providedAmount);
            Campaign savedCampaign = createTestCampaign(TEST_CAMPAIGN_ID, "New Campaign");
            CampaignResponseDTO expectedDTO = createResponseDTO(TEST_CAMPAIGN_ID, "New Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toEntity(any(CreateCampaignRequestDTO.class), eq(testUser)))
                        .thenReturn(savedCampaign);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(savedCampaign))
                        .thenReturn(expectedDTO);

                // When
                campaignService.createCampaign(createCampaignDTO);

                // Then
                assertThat(createCampaignDTO.getAmountRaised()).isEqualTo(providedAmount);
            }
        }

        @Test
        @DisplayName("Should throw RuntimeException when user is not found")
        void createCampaign_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignService.createCampaign(createCampaignDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(USER_NOT_FOUND_MESSAGE + TEST_USER_ID);

            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @Test
        @DisplayName("Should retrieve authenticated user before creating campaign")
        void createCampaign_ShouldRetrieveAuthenticatedUser() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toEntity(any(), any())).thenReturn(testCampaign);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any())).thenReturn(campaignResponseDTO);

                // When
                campaignService.createCampaign(createCampaignDTO);

                // Then
                verify(authenticatedUserService).getAuthenticatedUserId();
                verify(userRepository).findById(TEST_USER_ID);
            }
        }
    }

    @Nested
    @DisplayName("updateCampaign() tests")
    class UpdateCampaignTests {

        @Test
        @DisplayName("Should update campaign successfully with valid data")
        void updateCampaign_WithValidData_ShouldUpdateCampaign() {
            // Given
            Campaign updatedCampaign = createTestCampaign(TEST_CAMPAIGN_ID, "Updated Campaign");
            CampaignResponseDTO expectedDTO = createResponseDTO(TEST_CAMPAIGN_ID, "Updated Campaign");

            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(updatedCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(updatedCampaign))
                        .thenReturn(expectedDTO);

                // When
                CampaignResponseDTO result = campaignService.updateCampaign(TEST_CAMPAIGN_ID, updateCampaignDTO);

                // Then
                assertThat(result)
                        .isNotNull()
                        .isEqualTo(expectedDTO);

                verify(campaignServiceValidation).findCampaignByIdOrThrow(TEST_CAMPAIGN_ID);
                verify(campaignServiceValidation).updateCampaignFields(testCampaign, updateCampaignDTO);
                verify(campaignServiceValidation).updateGoalFields(testGoal, updateCampaignDTO);
                verify(campaignRepository).save(testCampaign);
            }
        }

        @Test
        @DisplayName("Should update campaign fields via validation service")
        void updateCampaign_ShouldUpdateCampaignFields() {
            // Given
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.updateCampaign(TEST_CAMPAIGN_ID, updateCampaignDTO);

                // Then
                verify(campaignServiceValidation).updateCampaignFields(testCampaign, updateCampaignDTO);
            }
        }

        @Test
        @DisplayName("Should update goal fields via validation service")
        void updateCampaign_ShouldUpdateGoalFields() {
            // Given
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.updateCampaign(TEST_CAMPAIGN_ID, updateCampaignDTO);

                // Then
                verify(campaignServiceValidation).updateGoalFields(testGoal, updateCampaignDTO);
            }
        }

        @Test
        @DisplayName("Should find campaign before updating")
        void updateCampaign_ShouldFindCampaignFirst() {
            // Given
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.updateCampaign(TEST_CAMPAIGN_ID, updateCampaignDTO);

                // Then
                verify(campaignServiceValidation).findCampaignByIdOrThrow(TEST_CAMPAIGN_ID);
            }
        }

        @Test
        @DisplayName("Should save campaign after updating fields")
        void updateCampaign_ShouldSaveCampaign() {
            // Given
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.updateCampaign(TEST_CAMPAIGN_ID, updateCampaignDTO);

                // Then
                verify(campaignRepository).save(testCampaign);
            }
        }
    }

    @Nested
    @DisplayName("deleteCampaign() tests")
    class DeleteCampaignTests {

        @Test
        @DisplayName("Should delete campaign successfully when campaign exists")
        void deleteCampaign_WithValidId_ShouldDeleteCampaign() {
            // Given
            when(campaignRepository.findById(TEST_CAMPAIGN_ID)).thenReturn(Optional.of(testCampaign));

            // When
            campaignService.deleteCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(campaignRepository).findById(TEST_CAMPAIGN_ID);
            verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("Should throw RuntimeException when campaign is not found")
        void deleteCampaign_WithNonExistentCampaign_ShouldThrowException() {
            // Given
            when(campaignRepository.findById(TEST_CAMPAIGN_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> campaignService.deleteCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CAMPAIGN_NOT_FOUND_MESSAGE);

            verify(campaignRepository, never()).delete(any(Campaign.class));
        }

        @Test
        @DisplayName("Should find campaign before deleting")
        void deleteCampaign_ShouldFindCampaignFirst() {
            // Given
            when(campaignRepository.findById(TEST_CAMPAIGN_ID)).thenReturn(Optional.of(testCampaign));

            // When
            campaignService.deleteCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(campaignRepository).findById(TEST_CAMPAIGN_ID);
        }

        @Test
        @DisplayName("Should use soft delete via repository delete method")
        void deleteCampaign_ShouldUseSoftDelete() {
            // Given
            when(campaignRepository.findById(TEST_CAMPAIGN_ID)).thenReturn(Optional.of(testCampaign));

            // When
            campaignService.deleteCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(campaignRepository).delete(testCampaign);
        }
    }

    private Campaign createTestCampaign(Long id, String name) {
        Campaign campaign = new Campaign();
        campaign.setId(id);
        campaign.setName(name);
        campaign.setDescription("Description for " + name);
        campaign.setUser(testUser);
        campaign.setGoal(testGoal);
        return campaign;
    }

    private CampaignResponseDTO createResponseDTO(Long id, String name) {
        CampaignResponseDTO dto = new CampaignResponseDTO();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }
}
