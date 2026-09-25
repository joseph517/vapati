package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CategoryDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignCategory;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.mapper.CampaignMapper;
import com.vaPaTi.vaPaTi.repository.CampaignCategoryRepository;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.CampaignStatusHistoryRepository;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.CampaignServiceValidation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

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
    @Mock
    private CampaignAuthorizationService campaignAuthorizationService;
    @Mock
    private CampaignCategoryRepository campaignCategoryRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CampaignStatusHistoryService campaignStatusHistoryService;
    @Mock
    private CampaignStatusHistoryRepository campaignStatusHistoryRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CampaignService campaignService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_CAMPAIGN_ID = 1L;
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
        testGoal.setStatus(CampaignStatus.ACTIVE);

        testCampaign = new Campaign();
        testCampaign.setId(TEST_CAMPAIGN_ID);
        testCampaign.setUser(testUser);
        testCampaign.setName("Test Campaign");
        testCampaign.setDescription("Test Description");
        testCampaign.setGoal(testGoal);

        createCampaignDTO = new CreateCampaignRequestDTO();
        createCampaignDTO.setName("New Campaign");
        createCampaignDTO.setDescription("New Description");
        createCampaignDTO.setCategoryIds(List.of(1L));

        updateCampaignDTO = new UpdateCampaignRequestDTO();
        updateCampaignDTO.setName("Updated Campaign");
        updateCampaignDTO.setDescription("Updated Description");
        updateCampaignDTO.setCategoryIds(List.of(1L));

        campaignResponseDTO = new CampaignResponseDTO();
        campaignResponseDTO.setId(TEST_CAMPAIGN_ID);
        campaignResponseDTO.setName("Test Campaign");
    }

    // callerId null means anonymous
    private void givenCaller(Long callerId, boolean isAdmin) {
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.ofNullable(callerId));
        when(campaignAuthorizationService.isAdmin(callerId)).thenReturn(isAdmin);
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

            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findAllVisibleTo(TEST_USER_ID)).thenReturn(campaigns);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(campaign1), any())).thenReturn(dto1);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(campaign2), any())).thenReturn(dto2);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(campaign3), any())).thenReturn(dto3);

                // When
                List<CampaignResponseDTO> result = campaignService.getAllCampaigns();

                // Then
                assertThat(result)
                        .isNotNull()
                        .hasSize(3)
                        .containsExactly(dto1, dto2, dto3);

                mapperMock.verify(() -> CampaignMapper.toResponseDTO(eq(campaign1), any()));
                mapperMock.verify(() -> CampaignMapper.toResponseDTO(eq(campaign2), any()));
                mapperMock.verify(() -> CampaignMapper.toResponseDTO(eq(campaign3), any()));
            }
        }

        @Test
        @DisplayName("Should return empty list when no campaigns exist")
        void getAllCampaigns_WithNoCampaigns_ShouldReturnEmptyList() {
            // Given
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findAllVisibleTo(TEST_USER_ID)).thenReturn(List.of());

            // When
            List<CampaignResponseDTO> result = campaignService.getAllCampaigns();

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        @DisplayName("Should use the visibility query with the caller id for a regular user")
        void getAllCampaigns_WhenRegularUser_ShouldUseFindAllVisibleTo() {
            // Given
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findAllVisibleTo(TEST_USER_ID)).thenReturn(List.of());

            // When
            campaignService.getAllCampaigns();

            // Then
            verify(campaignRepository, times(1)).findAllVisibleTo(TEST_USER_ID);
            verify(campaignRepository, never()).findAllWithActiveOwner();
        }

        @Test
        @DisplayName("Should use the visibility query with a null caller id for an anonymous caller")
        void getAllCampaigns_WhenAnonymous_ShouldUseFindAllVisibleToWithNull() {
            // Given
            givenCaller(null, false);
            when(campaignRepository.findAllVisibleTo(null)).thenReturn(List.of());

            // When
            campaignService.getAllCampaigns();

            // Then
            verify(campaignRepository, times(1)).findAllVisibleTo(null);
            verify(campaignRepository, never()).findAllWithActiveOwner();
            verify(authenticatedUserService, never()).getAuthenticatedUserId();
        }

        @Test
        @DisplayName("Should use findAllWithActiveOwner for an admin")
        void getAllCampaigns_WhenAdmin_ShouldUseFindAllWithActiveOwner() {
            // Given
            givenCaller(TEST_USER_ID, true);
            when(campaignRepository.findAllWithActiveOwner()).thenReturn(List.of());

            // When
            campaignService.getAllCampaigns();

            // Then
            verify(campaignRepository, times(1)).findAllWithActiveOwner();
            verify(campaignRepository, never()).findAllVisibleTo(any());
        }

        @Test
        @DisplayName("Should not use the unfiltered findAll, which includes campaigns of deleted owners")
        void getAllCampaigns_ShouldNotUseUnfilteredFindAll() {
            // Given
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findAllVisibleTo(TEST_USER_ID)).thenReturn(List.of());

            // When
            campaignService.getAllCampaigns();

            // Then
            verify(campaignRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("getCampaignById() tests")
    class GetCampaignByIdTests {

        @Test
        @DisplayName("Should return mapped campaign DTO looked up with the caller id")
        void getCampaignById_WithExistingCampaign_ShouldReturnMappedDTO() {
            // Given
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_USER_ID));
            when(campaignServiceValidation.findVisibleCampaignByIdOrThrow(TEST_CAMPAIGN_ID, TEST_USER_ID)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(testCampaign), any())).thenReturn(campaignResponseDTO);

                // When
                CampaignResponseDTO result = campaignService.getCampaignById(TEST_CAMPAIGN_ID);

                // Then
                assertThat(result)
                        .isNotNull()
                        .isEqualTo(campaignResponseDTO);

                verify(campaignServiceValidation).findVisibleCampaignByIdOrThrow(TEST_CAMPAIGN_ID, TEST_USER_ID);
                verify(campaignServiceValidation, never()).findCampaignByIdOrThrow(any());
                mapperMock.verify(() -> CampaignMapper.toResponseDTO(eq(testCampaign), any()));
            }
        }

        @Test
        @DisplayName("Should look up the campaign with a null caller id when the caller is anonymous")
        void getCampaignById_WhenAnonymous_ShouldLookUpWithNullCallerId() {
            // Given
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
            when(campaignServiceValidation.findVisibleCampaignByIdOrThrow(TEST_CAMPAIGN_ID, null)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(testCampaign), any())).thenReturn(campaignResponseDTO);

                // When
                CampaignResponseDTO result = campaignService.getCampaignById(TEST_CAMPAIGN_ID);

                // Then
                assertThat(result).isEqualTo(campaignResponseDTO);
                verify(campaignServiceValidation).findVisibleCampaignByIdOrThrow(TEST_CAMPAIGN_ID, null);
                verify(authenticatedUserService, never()).getAuthenticatedUserId();
            }
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the campaign is not visible or does not exist")
        void getCampaignById_WithNonVisibleCampaign_ShouldThrowException() {
            // Given
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_USER_ID));
            when(campaignServiceValidation.findVisibleCampaignByIdOrThrow(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenThrow(new com.vaPaTi.vaPaTi.exception.ResourceNotFoundException(CAMPAIGN_NOT_FOUND_MESSAGE));

            // When & Then
            assertThatThrownBy(() -> campaignService.getCampaignById(TEST_CAMPAIGN_ID))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.ResourceNotFoundException.class)
                    .hasMessage(CAMPAIGN_NOT_FOUND_MESSAGE);
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
            when(campaignRepository.findByUserIdWithDetails(TEST_USER_ID)).thenReturn(userCampaigns);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(campaign1), any())).thenReturn(dto1);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(campaign2), any())).thenReturn(dto2);

                // When
                List<CampaignResponseDTO> result = campaignService.getCampaignsByAuthenticatedUser();

                // Then
                assertThat(result)
                        .isNotNull()
                        .hasSize(2)
                        .containsExactly(dto1, dto2);

                verify(authenticatedUserService).getAuthenticatedUserId();
                verify(campaignRepository).findByUserIdWithDetails(TEST_USER_ID);
            }
        }

        @Test
        @DisplayName("Should return empty list when user has no campaigns")
        void getCampaignsByAuthenticatedUser_WithNoCampaigns_ShouldReturnEmptyList() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignRepository.findByUserIdWithDetails(TEST_USER_ID)).thenReturn(List.of());

            // When
            List<CampaignResponseDTO> result = campaignService.getCampaignsByAuthenticatedUser();

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEmpty();

            verify(campaignRepository).findByUserIdWithDetails(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should retrieve authenticated user ID before querying campaigns")
        void getCampaignsByAuthenticatedUser_ShouldGetAuthenticatedUserId() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignRepository.findByUserIdWithDetails(TEST_USER_ID)).thenReturn(List.of());

            // When
            campaignService.getCampaignsByAuthenticatedUser();

            // Then
            verify(authenticatedUserService).getAuthenticatedUserId();
        }
    }

    @Nested
    @DisplayName("Listings: categories loaded in batch")
    class ListingCategoriesBatchTests {

        private Category category(Long id) {
            return Category.builder().id(id).name("Category " + id).description("Description " + id).build();
        }

        private CampaignCategory campaignCategory(Campaign campaign, Category category) {
            return CampaignCategory.builder().campaign(campaign).category(category).build();
        }

        private List<Long> categoryIds(CampaignResponseDTO dto) {
            return dto.getCategories().stream().map(CategoryDTO::getId).toList();
        }

        @Test
        @DisplayName("getAllCampaigns: one findByCampaignIdIn with every campaign id, never findByCampaignId")
        void getAllCampaigns_ShouldLoadCategoriesInOneQuery() {
            Campaign campaign1 = createTestCampaign(1L, "Campaign 1");
            Campaign campaign2 = createTestCampaign(2L, "Campaign 2");
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findAllVisibleTo(TEST_USER_ID)).thenReturn(List.of(campaign1, campaign2));

            campaignService.getAllCampaigns();

            verify(campaignCategoryRepository, times(1)).findByCampaignIdIn(List.of(1L, 2L));
            verify(campaignCategoryRepository, never()).findByCampaignId(any());
        }

        @Test
        @DisplayName("getCampaignsByStatus: one findByCampaignIdIn with every campaign id, never findByCampaignId")
        void getCampaignsByStatus_ShouldLoadCategoriesInOneQuery() {
            Campaign campaign1 = createTestCampaign(1L, "Campaign 1");
            Campaign campaign2 = createTestCampaign(2L, "Campaign 2");
            when(campaignServiceValidation.parseStatus("ACTIVE")).thenReturn(CampaignStatus.ACTIVE);
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findByGoalStatusVisibleTo(CampaignStatus.ACTIVE, TEST_USER_ID))
                    .thenReturn(List.of(campaign1, campaign2));

            campaignService.getCampaignsByStatus("ACTIVE");

            verify(campaignCategoryRepository, times(1)).findByCampaignIdIn(List.of(1L, 2L));
            verify(campaignCategoryRepository, never()).findByCampaignId(any());
        }

        @Test
        @DisplayName("getCampaignsByCategoryId: one findByCampaignIdIn with every campaign id, never findByCampaignId")
        void getCampaignsByCategoryId_ShouldLoadCategoriesInOneQuery() {
            Campaign campaign1 = createTestCampaign(1L, "Campaign 1");
            Campaign campaign2 = createTestCampaign(2L, "Campaign 2");
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findByCategoryIdVisibleTo(5L, TEST_USER_ID)).thenReturn(List.of(campaign1, campaign2));

            campaignService.getCampaignsByCategoryId(5L);

            verify(campaignCategoryRepository, times(1)).findByCampaignIdIn(List.of(1L, 2L));
            verify(campaignCategoryRepository, never()).findByCampaignId(any());
        }

        @Test
        @DisplayName("getCampaignsByAuthenticatedUser: findByUserIdWithDetails and one findByCampaignIdIn, never findByUserId or findByCampaignId")
        void getCampaignsByAuthenticatedUser_ShouldUseFetchQueryAndLoadCategoriesInOneQuery() {
            Campaign campaign1 = createTestCampaign(1L, "Campaign 1");
            Campaign campaign2 = createTestCampaign(2L, "Campaign 2");
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignRepository.findByUserIdWithDetails(TEST_USER_ID)).thenReturn(List.of(campaign1, campaign2));

            campaignService.getCampaignsByAuthenticatedUser();

            verify(campaignRepository).findByUserIdWithDetails(TEST_USER_ID);
            verify(campaignRepository, never()).findByUserId(any());
            verify(campaignCategoryRepository, times(1)).findByCampaignIdIn(List.of(1L, 2L));
            verify(campaignCategoryRepository, never()).findByCampaignId(any());
        }

        @Test
        @DisplayName("Each DTO gets only its own categories, in the order of the campaign list; a campaign without categories gets []")
        void listing_ShouldGroupCategoriesByCampaignAndKeepOrder() {
            Campaign campaign1 = createTestCampaign(1L, "Campaign 1");
            Campaign campaign2 = createTestCampaign(2L, "Campaign 2");
            Campaign campaign3 = createTestCampaign(3L, "Campaign 3");
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findAllVisibleTo(TEST_USER_ID)).thenReturn(List.of(campaign2, campaign3, campaign1));
            // Rows come back interleaved: the grouping must not depend on the query order
            when(campaignCategoryRepository.findByCampaignIdIn(List.of(2L, 3L, 1L))).thenReturn(List.of(
                    campaignCategory(campaign1, category(10L)),
                    campaignCategory(campaign2, category(20L)),
                    campaignCategory(campaign1, category(11L)),
                    campaignCategory(campaign2, category(10L))));

            List<CampaignResponseDTO> result = campaignService.getAllCampaigns();

            assertThat(result).extracting(CampaignResponseDTO::getId).containsExactly(2L, 3L, 1L);
            assertThat(categoryIds(result.get(0))).containsExactly(20L, 10L);
            assertThat(categoryIds(result.get(1))).isEmpty();
            assertThat(categoryIds(result.get(2))).containsExactly(10L, 11L);
        }

        @Test
        @DisplayName("An empty listing doesn't query the categories (SQL Server rejects IN ())")
        void listing_WithNoCampaigns_ShouldNotQueryCategories() {
            givenCaller(null, false);
            when(campaignRepository.findAllVisibleTo(null)).thenReturn(List.of());

            List<CampaignResponseDTO> result = campaignService.getAllCampaigns();

            assertThat(result).isEmpty();
            verify(campaignCategoryRepository, never()).findByCampaignIdIn(any());
        }

        @Test
        @DisplayName("1001 campaigns: findByCampaignIdIn is called twice, with 1000 and 1 ids")
        void listing_WithMoreThanChunkSize_ShouldQueryCategoriesInChunks() {
            List<Campaign> campaigns = LongStream.rangeClosed(1, 1001)
                    .mapToObj(id -> createTestCampaign(id, "Campaign " + id))
                    .toList();
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findAllVisibleTo(TEST_USER_ID)).thenReturn(campaigns);

            List<CampaignResponseDTO> result = campaignService.getAllCampaigns();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(campaignCategoryRepository, times(2)).findByCampaignIdIn(idsCaptor.capture());
            assertThat(idsCaptor.getAllValues().get(0)).hasSize(1000).first().isEqualTo(1L);
            assertThat(idsCaptor.getAllValues().get(1)).containsExactly(1001L);
            assertThat(result).hasSize(1001);
        }

        @Test
        @DisplayName("getCampaignById keeps using findByCampaignId")
        void getCampaignById_ShouldKeepUsingFindByCampaignId() {
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(TEST_USER_ID));
            when(campaignServiceValidation.findVisibleCampaignByIdOrThrow(TEST_CAMPAIGN_ID, TEST_USER_ID)).thenReturn(testCampaign);

            campaignService.getCampaignById(TEST_CAMPAIGN_ID);

            verify(campaignCategoryRepository).findByCampaignId(TEST_CAMPAIGN_ID);
            verify(campaignCategoryRepository, never()).findByCampaignIdIn(any());
        }
    }

    @Nested
    @DisplayName("createCampaign() tests")
    class CreateCampaignTests {

        @Test
        @DisplayName("Should create campaign successfully with valid data")
        void createCampaign_WithValidDTO_ShouldCreateCampaign() {
            // Given
            Campaign savedCampaign = createTestCampaign(TEST_CAMPAIGN_ID, "New Campaign");
            CampaignResponseDTO expectedDTO = createResponseDTO(TEST_CAMPAIGN_ID, "New Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toEntity(createCampaignDTO, testUser))
                        .thenReturn(savedCampaign);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(savedCampaign), any()))
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
                mapperMock.verify(() -> CampaignMapper.toResponseDTO(eq(savedCampaign), any()));
            }
        }

        @Test
        @DisplayName("Should start the new campaign's goal with amountRaised 0 and ACTIVE")
        void createCampaign_ShouldStartGoalWithZeroRaisedAndActive() {
            // Given
            createCampaignDTO.setAmountGoal(new BigDecimal("100.00"));
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CampaignResponseDTO result = campaignService.createCampaign(createCampaignDTO);

            // Then
            ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
            verify(campaignRepository).save(campaignCaptor.capture());
            Goal savedGoal = campaignCaptor.getValue().getGoal();
            assertThat(savedGoal.getAmountRaised()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(savedGoal.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
            assertThat(result.getAmountRaised()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
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
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any())).thenReturn(campaignResponseDTO);

                // When
                campaignService.createCampaign(createCampaignDTO);

                // Then
                verify(authenticatedUserService).getAuthenticatedUserId();
                verify(userRepository).findById(TEST_USER_ID);
            }
        }

        @Test
        @DisplayName("Should validate category ids and create a CampaignCategory per id")
        void createCampaign_WithValidCategoryIds_ShouldCreateCampaignCategories() {
            // Given
            com.vaPaTi.vaPaTi.entity.Category category1 = new com.vaPaTi.vaPaTi.entity.Category();
            category1.setId(1L);
            Campaign savedCampaign = createTestCampaign(TEST_CAMPAIGN_ID, "New Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);
            when(categoryRepository.findAllById(createCampaignDTO.getCategoryIds())).thenReturn(List.of(category1));

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toEntity(any(), any())).thenReturn(savedCampaign);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any())).thenReturn(campaignResponseDTO);

                // When
                campaignService.createCampaign(createCampaignDTO);

                // Then
                verify(campaignServiceValidation).validateCategoryIds(createCampaignDTO.getCategoryIds());
                verify(categoryRepository).findAllById(createCampaignDTO.getCategoryIds());
                verify(campaignCategoryRepository).saveAll(argThat(list -> {
                    List<?> saved = (List<?>) list;
                    return saved.size() == 1;
                }));
            }
        }

        @Test
        @DisplayName("Should record a null -> ACTIVE status history entry authored by the owner")
        void createCampaign_ShouldRecordStatusHistoryEntry() {
            // Given
            Campaign savedCampaign = createTestCampaign(TEST_CAMPAIGN_ID, "New Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toEntity(any(), any())).thenReturn(savedCampaign);
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any())).thenReturn(campaignResponseDTO);

                // When
                campaignService.createCampaign(createCampaignDTO);

                // Then
                verify(campaignStatusHistoryService)
                        .recordTransition(savedCampaign, null, CampaignStatus.ACTIVE, TEST_USER_ID);
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

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(updatedCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(updatedCampaign), any()))
                        .thenReturn(expectedDTO);

                // When
                CampaignResponseDTO result = campaignService.updateCampaign(TEST_CAMPAIGN_ID, updateCampaignDTO);

                // Then
                assertThat(result)
                        .isNotNull()
                        .isEqualTo(expectedDTO);

                verify(authenticatedUserService).getAuthenticatedUserId();
                verify(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
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
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
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
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
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
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
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
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.updateCampaign(TEST_CAMPAIGN_ID, updateCampaignDTO);

                // Then
                verify(campaignRepository).save(testCampaign);
            }
        }

        @Test
        @DisplayName("Should replace the campaign's categories entirely")
        void updateCampaign_ShouldReplaceCategories() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            com.vaPaTi.vaPaTi.entity.Category newCategory = new com.vaPaTi.vaPaTi.entity.Category();
            newCategory.setId(2L);
            updateCampaignDTO.setCategoryIds(List.of(2L));
            when(categoryRepository.findAllById(updateCampaignDTO.getCategoryIds())).thenReturn(List.of(newCategory));

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.updateCampaign(TEST_CAMPAIGN_ID, updateCampaignDTO);

                // Then
                verify(campaignServiceValidation).validateCategoryIds(updateCampaignDTO.getCategoryIds());
                verify(campaignCategoryRepository).deleteByCampaignId(testCampaign.getId());
                verify(categoryRepository).findAllById(updateCampaignDTO.getCategoryIds());
                verify(campaignCategoryRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
            }
        }
    }

    @Nested
    @DisplayName("getCampaignsByCategoryId() tests")
    class GetCampaignsByCategoryIdTests {

        private static final Long CATEGORY_ID = 5L;

        @Test
        @DisplayName("Should return only campaigns associated with the given category")
        void getCampaignsByCategoryId_WithMatchingCampaigns_ShouldReturnFilteredList() {
            // Given
            Campaign campaign1 = createTestCampaign(1L, "Campaign 1");
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findByCategoryIdVisibleTo(CATEGORY_ID, TEST_USER_ID)).thenReturn(List.of(campaign1));

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any())).thenReturn(campaignResponseDTO);

                // When
                List<CampaignResponseDTO> result = campaignService.getCampaignsByCategoryId(CATEGORY_ID);

                // Then
                assertThat(result).hasSize(1);
                verify(campaignRepository).findByCategoryIdVisibleTo(CATEGORY_ID, TEST_USER_ID);
                verify(campaignRepository, never()).findByCategoryIdWithActiveOwner(any());
            }
        }

        @Test
        @DisplayName("Should return empty list when no campaign is associated with the category")
        void getCampaignsByCategoryId_WithNoMatches_ShouldReturnEmptyList() {
            // Given
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findByCategoryIdVisibleTo(CATEGORY_ID, TEST_USER_ID)).thenReturn(List.of());

            // When
            List<CampaignResponseDTO> result = campaignService.getCampaignsByCategoryId(CATEGORY_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should use the visibility query with a null caller id for an anonymous caller")
        void getCampaignsByCategoryId_WhenAnonymous_ShouldUseVisibleToWithNull() {
            // Given
            givenCaller(null, false);
            when(campaignRepository.findByCategoryIdVisibleTo(CATEGORY_ID, null)).thenReturn(List.of());

            // When
            campaignService.getCampaignsByCategoryId(CATEGORY_ID);

            // Then
            verify(campaignRepository).findByCategoryIdVisibleTo(CATEGORY_ID, null);
            verify(campaignRepository, never()).findByCategoryIdWithActiveOwner(any());
        }

        @Test
        @DisplayName("Should use findByCategoryIdWithActiveOwner for an admin")
        void getCampaignsByCategoryId_WhenAdmin_ShouldUseWithActiveOwner() {
            // Given
            givenCaller(TEST_USER_ID, true);
            when(campaignRepository.findByCategoryIdWithActiveOwner(CATEGORY_ID)).thenReturn(List.of());

            // When
            campaignService.getCampaignsByCategoryId(CATEGORY_ID);

            // Then
            verify(campaignRepository).findByCategoryIdWithActiveOwner(CATEGORY_ID);
            verify(campaignRepository, never()).findByCategoryIdVisibleTo(any(), any());
        }

        @Test
        @DisplayName("Should use a single campaign query instead of loading the campaign_category rows")
        void getCampaignsByCategoryId_ShouldUseSingleCampaignQuery() {
            // Given
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findByCategoryIdVisibleTo(CATEGORY_ID, TEST_USER_ID)).thenReturn(List.of());

            // When
            campaignService.getCampaignsByCategoryId(CATEGORY_ID);

            // Then
            verify(campaignRepository, never()).findAllById(any());
            verifyNoInteractions(campaignCategoryRepository);
        }
    }

    @Nested
    @DisplayName("getCampaignsByStatus() tests")
    class GetCampaignsByStatusTests {

        @Test
        @DisplayName("Should return only campaigns whose goal has the requested status")
        void getCampaignsByStatus_WithMatchingCampaigns_ShouldReturnFilteredList() {
            // Given
            Campaign campaign1 = createTestCampaign(1L, "Campaign 1");
            when(campaignServiceValidation.parseStatus("CLOSED")).thenReturn(CampaignStatus.CLOSED);
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findByGoalStatusVisibleTo(CampaignStatus.CLOSED, TEST_USER_ID)).thenReturn(List.of(campaign1));

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any())).thenReturn(campaignResponseDTO);

                // When
                List<CampaignResponseDTO> result = campaignService.getCampaignsByStatus("CLOSED");

                // Then
                assertThat(result).hasSize(1);
                verify(campaignServiceValidation).parseStatus("CLOSED");
                verify(campaignRepository).findByGoalStatusVisibleTo(CampaignStatus.CLOSED, TEST_USER_ID);
                verify(campaignRepository, never()).findByGoalStatusWithActiveOwner(any());
            }
        }

        @Test
        @DisplayName("Should use the visibility query with a null caller id for an anonymous caller")
        void getCampaignsByStatus_WhenAnonymous_ShouldUseVisibleToWithNull() {
            // Given
            when(campaignServiceValidation.parseStatus("CLOSED")).thenReturn(CampaignStatus.CLOSED);
            givenCaller(null, false);
            when(campaignRepository.findByGoalStatusVisibleTo(CampaignStatus.CLOSED, null)).thenReturn(List.of());

            // When
            List<CampaignResponseDTO> result = campaignService.getCampaignsByStatus("CLOSED");

            // Then
            assertThat(result).isEmpty();
            verify(campaignRepository).findByGoalStatusVisibleTo(CampaignStatus.CLOSED, null);
            verify(campaignRepository, never()).findByGoalStatusWithActiveOwner(any());
        }

        @Test
        @DisplayName("Should use findByGoalStatusWithActiveOwner for an admin")
        void getCampaignsByStatus_WhenAdmin_ShouldUseWithActiveOwner() {
            // Given
            when(campaignServiceValidation.parseStatus("CLOSED")).thenReturn(CampaignStatus.CLOSED);
            givenCaller(TEST_USER_ID, true);
            when(campaignRepository.findByGoalStatusWithActiveOwner(CampaignStatus.CLOSED)).thenReturn(List.of());

            // When
            campaignService.getCampaignsByStatus("CLOSED");

            // Then
            verify(campaignRepository).findByGoalStatusWithActiveOwner(CampaignStatus.CLOSED);
            verify(campaignRepository, never()).findByGoalStatusVisibleTo(any(), any());
        }

        @Test
        @DisplayName("Should propagate exception when status value is invalid")
        void getCampaignsByStatus_WithInvalidValue_ShouldThrowException() {
            // Given
            when(campaignServiceValidation.parseStatus("FOO"))
                    .thenThrow(new com.vaPaTi.vaPaTi.exception.MessageException("Invalid campaign status: FOO"));

            // When & Then
            assertThatThrownBy(() -> campaignService.getCampaignsByStatus("FOO"))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.MessageException.class)
                    .hasMessage("Invalid campaign status: FOO");

            verify(campaignRepository, never()).findByGoalStatusWithActiveOwner(any());
            verify(campaignRepository, never()).findByGoalStatusVisibleTo(any(), any());
        }

        @Test
        @DisplayName("Should return empty list when no campaign matches the status")
        void getCampaignsByStatus_WithNoMatches_ShouldReturnEmptyList() {
            // Given
            when(campaignServiceValidation.parseStatus("ACTIVE")).thenReturn(CampaignStatus.ACTIVE);
            givenCaller(TEST_USER_ID, false);
            when(campaignRepository.findByGoalStatusVisibleTo(CampaignStatus.ACTIVE, TEST_USER_ID)).thenReturn(List.of());

            // When
            List<CampaignResponseDTO> result = campaignService.getCampaignsByStatus("ACTIVE");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteCampaign() tests")
    class DeleteCampaignTests {

        @Test
        @DisplayName("Should delete campaign successfully when campaign exists")
        void deleteCampaign_WithValidId_ShouldDeleteCampaign() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);

            // When
            campaignService.deleteCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(authenticatedUserService).getAuthenticatedUserId();
            verify(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            verify(campaignServiceValidation).findCampaignByIdOrThrow(TEST_CAMPAIGN_ID);
            verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when campaign is not found")
        void deleteCampaign_WithNonExistentCampaign_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID))
                    .thenThrow(new com.vaPaTi.vaPaTi.exception.ResourceNotFoundException(CAMPAIGN_NOT_FOUND_MESSAGE));

            // When & Then
            assertThatThrownBy(() -> campaignService.deleteCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.ResourceNotFoundException.class)
                    .hasMessage(CAMPAIGN_NOT_FOUND_MESSAGE);

            verify(campaignRepository, never()).delete(any(Campaign.class));
        }

        @Test
        @DisplayName("Should find campaign before deleting")
        void deleteCampaign_ShouldFindCampaignFirst() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);

            // When
            campaignService.deleteCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(campaignServiceValidation).findCampaignByIdOrThrow(TEST_CAMPAIGN_ID);
        }

        @Test
        @DisplayName("Should use soft delete via repository delete method")
        void deleteCampaign_ShouldUseSoftDelete() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);

            // When
            campaignService.deleteCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("Should record a status history entry when the goal was not already closed")
        void deleteCampaign_WithGoalNotClosed_ShouldRecordStatusHistoryEntry() {
            // Given
            testGoal.setStatus(CampaignStatus.ACTIVE);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);

            // When
            campaignService.deleteCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(campaignStatusHistoryService)
                    .recordTransition(testCampaign, CampaignStatus.ACTIVE, CampaignStatus.CLOSED, TEST_USER_ID);
        }

        @Test
        @DisplayName("Should not record a duplicate status history entry when the goal was already closed")
        void deleteCampaign_WithGoalAlreadyClosed_ShouldNotRecordStatusHistoryEntry() {
            // Given
            testGoal.setStatus(CampaignStatus.CLOSED);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);

            // When
            campaignService.deleteCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(campaignStatusHistoryService, never())
                    .recordTransition(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should delegate to closeAndSoftDelete with the authenticated user")
        void deleteCampaign_ShouldDelegateToCloseAndSoftDelete() {
            // Given
            CampaignService spyService = spy(campaignService);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);

            // When
            spyService.deleteCampaign(TEST_CAMPAIGN_ID);

            // Then
            verify(spyService).closeAndSoftDelete(testCampaign, TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("closeCampaign() tests")
    class CloseCampaignTests {

        @Test
        @DisplayName("Should close campaign successfully when goal is active")
        void closeCampaign_WithActiveGoal_ShouldCloseCampaign() {
            // Given
            CampaignResponseDTO expectedDTO = createResponseDTO(TEST_CAMPAIGN_ID, "Test Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(testCampaign), any()))
                        .thenReturn(expectedDTO);

                // When
                CampaignResponseDTO result = campaignService.closeCampaign(TEST_CAMPAIGN_ID);

                // Then
                assertThat(result).isNotNull().isEqualTo(expectedDTO);
                assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.CLOSED);
                verify(authenticatedUserService).getAuthenticatedUserId();
                verify(campaignAuthorizationService).getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID);
                verify(campaignRepository).save(testCampaign);
            }
        }

        @Test
        @DisplayName("Should close campaign successfully when goal is completed")
        void closeCampaign_WithCompletedGoal_ShouldCloseCampaign() {
            // Given
            testGoal.setStatus(CampaignStatus.COMPLETED);
            CampaignResponseDTO expectedDTO = createResponseDTO(TEST_CAMPAIGN_ID, "Test Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(testCampaign), any()))
                        .thenReturn(expectedDTO);

                // When
                CampaignResponseDTO result = campaignService.closeCampaign(TEST_CAMPAIGN_ID);

                // Then
                assertThat(result).isNotNull().isEqualTo(expectedDTO);
                assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.CLOSED);
            }
        }

        @Test
        @DisplayName("Should throw MessageException when campaign has no goal")
        void closeCampaign_WithNoGoal_ShouldThrowException() {
            // Given
            testCampaign.setGoal(null);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);

            // When & Then
            assertThatThrownBy(() -> campaignService.closeCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.MessageException.class)
                    .hasMessage("Campaign does not have a goal");

            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @Test
        @DisplayName("Should throw MessageException when goal is already closed")
        void closeCampaign_WithInactiveGoal_ShouldThrowException() {
            // Given
            testGoal.setStatus(CampaignStatus.CLOSED);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);

            // When & Then
            assertThatThrownBy(() -> campaignService.closeCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.MessageException.class)
                    .hasMessage("Campaign goal is already closed");

            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @Test
        @DisplayName("Should validate authorization before closing campaign")
        void closeCampaign_ShouldValidateAuthorization() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.closeCampaign(TEST_CAMPAIGN_ID);

                // Then
                verify(campaignAuthorizationService).getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID);
            }
        }

        @Test
        @DisplayName("Should set goal active to false when closing campaign")
        void closeCampaign_ShouldSetGoalToInactive() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.closeCampaign(TEST_CAMPAIGN_ID);

                // Then
                assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.CLOSED);
            }
        }

        @Test
        @DisplayName("Should save campaign after setting goal to inactive")
        void closeCampaign_ShouldSaveCampaign() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.closeCampaign(TEST_CAMPAIGN_ID);

                // Then
                verify(campaignRepository).save(testCampaign);
            }
        }

        @Test
        @DisplayName("Should record an ACTIVE -> CLOSED status history entry authored by the acting user")
        void closeCampaign_ShouldRecordStatusHistoryEntry() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.closeCampaign(TEST_CAMPAIGN_ID);

                // Then
                verify(campaignStatusHistoryService)
                        .recordTransition(testCampaign, CampaignStatus.ACTIVE, CampaignStatus.CLOSED, TEST_USER_ID);
            }
        }

        @Test
        @DisplayName("Should not record a status history entry when closing throws")
        void closeCampaign_WhenThrows_ShouldNotRecordStatusHistoryEntry() {
            // Given
            testCampaign.setGoal(null);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);

            // When & Then
            assertThatThrownBy(() -> campaignService.closeCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.MessageException.class);

            verify(campaignStatusHistoryService, never())
                    .recordTransition(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("activateCampaign() tests")
    class ActivateCampaignTests {

        @Test
        @DisplayName("Should reactivate to ACTIVE when goal amount was not reached")
        void activateCampaign_WithClosedGoalBelowTarget_ShouldSetGoalActive() {
            // Given
            testGoal.setStatus(CampaignStatus.CLOSED);
            testGoal.setAmountGoal(new BigDecimal("1000.0"));
            testGoal.setAmountRaised(new BigDecimal("500.0"));
            when(campaignServiceValidation.statusForAmounts(testGoal)).thenCallRealMethod();
            CampaignResponseDTO expectedDTO = createResponseDTO(TEST_CAMPAIGN_ID, "Test Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(testCampaign), any()))
                        .thenReturn(expectedDTO);

                // When
                CampaignResponseDTO result = campaignService.activateCampaign(TEST_CAMPAIGN_ID);

                // Then
                assertThat(result).isNotNull().isEqualTo(expectedDTO);
                assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
            }
        }

        @Test
        @DisplayName("Should reactivate to COMPLETED when goal amount was already reached")
        void activateCampaign_WithClosedGoalAtOrAboveTarget_ShouldSetGoalCompleted() {
            // Given
            testGoal.setStatus(CampaignStatus.CLOSED);
            testGoal.setAmountGoal(new BigDecimal("1000.0"));
            testGoal.setAmountRaised(new BigDecimal("1000.0"));
            when(campaignServiceValidation.statusForAmounts(testGoal)).thenCallRealMethod();
            CampaignResponseDTO expectedDTO = createResponseDTO(TEST_CAMPAIGN_ID, "Test Campaign");

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(eq(testCampaign), any()))
                        .thenReturn(expectedDTO);

                // When
                CampaignResponseDTO result = campaignService.activateCampaign(TEST_CAMPAIGN_ID);

                // Then
                assertThat(result).isNotNull().isEqualTo(expectedDTO);
                assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
            }
        }

        @Test
        @DisplayName("Should throw MessageException when campaign has no goal")
        void activateCampaign_WithNoGoal_ShouldThrowException() {
            // Given
            testCampaign.setGoal(null);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);

            // When & Then
            assertThatThrownBy(() -> campaignService.activateCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.MessageException.class)
                    .hasMessage("Campaign does not have a goal");

            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @Test
        @DisplayName("Should throw MessageException when goal is not closed (ACTIVE)")
        void activateCampaign_WithActiveGoal_ShouldThrowException() {
            // Given
            testGoal.setStatus(CampaignStatus.ACTIVE);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);

            // When & Then
            assertThatThrownBy(() -> campaignService.activateCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.MessageException.class)
                    .hasMessage("Campaign is not closed");

            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @Test
        @DisplayName("Should throw MessageException when goal is not closed (COMPLETED)")
        void activateCampaign_WithCompletedGoal_ShouldThrowException() {
            // Given
            testGoal.setStatus(CampaignStatus.COMPLETED);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);

            // When & Then
            assertThatThrownBy(() -> campaignService.activateCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.MessageException.class)
                    .hasMessage("Campaign is not closed");

            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @Test
        @DisplayName("Should throw MessageException when user is not owner nor admin")
        void activateCampaign_WhenNotAuthorized_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenThrow(new com.vaPaTi.vaPaTi.exception.MessageException("You are not authorized to perform this action"));

            // When & Then
            assertThatThrownBy(() -> campaignService.activateCampaign(TEST_CAMPAIGN_ID))
                    .isInstanceOf(com.vaPaTi.vaPaTi.exception.MessageException.class)
                    .hasMessage("You are not authorized to perform this action");

            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @Test
        @DisplayName("Should record a CLOSED -> ACTIVE status history entry authored by the acting user")
        void activateCampaign_WithGoalBelowTarget_ShouldRecordStatusHistoryEntry() {
            // Given
            testGoal.setStatus(CampaignStatus.CLOSED);
            testGoal.setAmountGoal(new BigDecimal("1000.0"));
            testGoal.setAmountRaised(new BigDecimal("500.0"));
            when(campaignServiceValidation.statusForAmounts(testGoal)).thenCallRealMethod();

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.activateCampaign(TEST_CAMPAIGN_ID);

                // Then
                verify(campaignStatusHistoryService)
                        .recordTransition(testCampaign, CampaignStatus.CLOSED, CampaignStatus.ACTIVE, TEST_USER_ID);
            }
        }

        @Test
        @DisplayName("Should record a CLOSED -> COMPLETED status history entry when target was already reached")
        void activateCampaign_WithGoalAtTarget_ShouldRecordStatusHistoryEntry() {
            // Given
            testGoal.setStatus(CampaignStatus.CLOSED);
            testGoal.setAmountGoal(new BigDecimal("1000.0"));
            testGoal.setAmountRaised(new BigDecimal("1000.0"));
            when(campaignServiceValidation.statusForAmounts(testGoal)).thenCallRealMethod();

            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID))
                    .thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            try (MockedStatic<CampaignMapper> mapperMock = mockStatic(CampaignMapper.class)) {
                mapperMock.when(() -> CampaignMapper.toResponseDTO(any(), any()))
                        .thenReturn(campaignResponseDTO);

                // When
                campaignService.activateCampaign(TEST_CAMPAIGN_ID);

                // Then
                verify(campaignStatusHistoryService)
                        .recordTransition(testCampaign, CampaignStatus.CLOSED, CampaignStatus.COMPLETED, TEST_USER_ID);
            }
        }
    }

    @Nested
    @DisplayName("Goal lock before status changes (P16)")
    class GoalLockTests {

        // Simulates a concurrent change (e.g. a donation) that the refresh with lock picks up from the database
        private void givenRefreshLoads(Goal goal, CampaignStatus realStatus, BigDecimal realAmountRaised) {
            doAnswer(invocation -> {
                goal.setStatus(realStatus);
                goal.setAmountRaised(realAmountRaised);
                return null;
            }).when(entityManager).refresh(goal, LockModeType.PESSIMISTIC_WRITE);
        }

        @Test
        @DisplayName("closeCampaign locks the goal before changing its status and records the real previous status")
        void closeCampaign_ShouldLockGoalBeforeChangingStatus() {
            // Given: ACTIVE in memory, a donation completed it meanwhile
            givenRefreshLoads(testGoal, CampaignStatus.COMPLETED, new BigDecimal("1000.00"));
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            // When
            campaignService.closeCampaign(TEST_CAMPAIGN_ID);

            // Then
            assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.CLOSED);
            InOrder inOrder = inOrder(entityManager, campaignRepository, campaignStatusHistoryService);
            inOrder.verify(entityManager).refresh(testGoal, LockModeType.PESSIMISTIC_WRITE);
            inOrder.verify(campaignRepository).save(testCampaign);
            inOrder.verify(campaignStatusHistoryService)
                    .recordTransition(testCampaign, CampaignStatus.COMPLETED, CampaignStatus.CLOSED, TEST_USER_ID);
        }

        @Test
        @DisplayName("activateCampaign locks the goal before deciding ACTIVE or COMPLETED with the real amount raised")
        void activateCampaign_ShouldLockGoalBeforeChangingStatus() {
            // Given: 500 of 1000 raised in memory, 1000 in the database
            testGoal.setStatus(CampaignStatus.CLOSED);
            testGoal.setAmountGoal(new BigDecimal("1000.00"));
            testGoal.setAmountRaised(new BigDecimal("500.00"));
            givenRefreshLoads(testGoal, CampaignStatus.CLOSED, new BigDecimal("1000.00"));
            when(campaignServiceValidation.statusForAmounts(testGoal)).thenCallRealMethod();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignAuthorizationService.getCampaignIfAuthorized(TEST_CAMPAIGN_ID, TEST_USER_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            // When
            campaignService.activateCampaign(TEST_CAMPAIGN_ID);

            // Then
            assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
            InOrder inOrder = inOrder(entityManager, campaignRepository, campaignStatusHistoryService);
            inOrder.verify(entityManager).refresh(testGoal, LockModeType.PESSIMISTIC_WRITE);
            inOrder.verify(campaignRepository).save(testCampaign);
            inOrder.verify(campaignStatusHistoryService)
                    .recordTransition(testCampaign, CampaignStatus.CLOSED, CampaignStatus.COMPLETED, TEST_USER_ID);
        }

        @Test
        @DisplayName("updateCampaign with amountGoal locks the goal before setting the new goal and recalculating")
        void updateCampaign_WithAmountGoal_ShouldLockGoalBeforeChangingStatus() {
            // Given: nothing raised in memory, 20 in the database; the new goal is 15
            testGoal.setAmountGoal(new BigDecimal("1000.00"));
            testGoal.setAmountRaised(BigDecimal.ZERO);
            givenRefreshLoads(testGoal, CampaignStatus.ACTIVE, new BigDecimal("20.00"));
            updateCampaignDTO.setAmountGoal(new BigDecimal("15.00"));
            doCallRealMethod().when(campaignServiceValidation).updateGoalFields(any(), any());
            when(campaignServiceValidation.statusForAmounts(testGoal)).thenCallRealMethod();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
            when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
            when(campaignRepository.save(testCampaign)).thenReturn(testCampaign);

            // When
            campaignService.updateCampaign(TEST_CAMPAIGN_ID, updateCampaignDTO);

            // Then: the refresh did not discard the new goal
            assertThat(testGoal.getAmountGoal()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
            InOrder inOrder = inOrder(entityManager, campaignServiceValidation, campaignStatusHistoryService);
            inOrder.verify(entityManager).refresh(testGoal, LockModeType.PESSIMISTIC_WRITE);
            inOrder.verify(campaignServiceValidation).updateGoalFields(testGoal, updateCampaignDTO);
            inOrder.verify(campaignStatusHistoryService)
                    .recordTransition(testCampaign, CampaignStatus.ACTIVE, CampaignStatus.COMPLETED, TEST_USER_ID);
        }

        @Test
        @DisplayName("closeAndSoftDelete locks the goal before changing its status and records the real previous status")
        void closeAndSoftDelete_ShouldLockGoalBeforeChangingStatus() {
            // Given: ACTIVE in memory, a donation completed it meanwhile
            givenRefreshLoads(testGoal, CampaignStatus.COMPLETED, new BigDecimal("1000.00"));

            // When
            campaignService.closeAndSoftDelete(testCampaign, TEST_USER_ID);

            // Then
            assertThat(testGoal.getStatus()).isEqualTo(CampaignStatus.CLOSED);
            InOrder inOrder = inOrder(entityManager, campaignStatusHistoryService, campaignRepository);
            inOrder.verify(entityManager).refresh(testGoal, LockModeType.PESSIMISTIC_WRITE);
            inOrder.verify(campaignStatusHistoryService)
                    .recordTransition(testCampaign, CampaignStatus.COMPLETED, CampaignStatus.CLOSED, TEST_USER_ID);
            inOrder.verify(campaignRepository).saveAndFlush(testCampaign);
            inOrder.verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("closeAllByOwner locks each goal before changing its status and records the real previous status")
        void closeAllByOwner_ShouldLockEachGoalBeforeChangingStatus() {
            // Given: both ACTIVE in memory; in the database one was completed and the other closed meanwhile
            Goal completedGoal = new Goal();
            completedGoal.setId(1L);
            completedGoal.setStatus(CampaignStatus.ACTIVE);
            Campaign completedCampaign = createTestCampaign(1L, "Completed meanwhile");
            completedCampaign.setGoal(completedGoal);

            Goal closedGoal = new Goal();
            closedGoal.setId(2L);
            closedGoal.setStatus(CampaignStatus.ACTIVE);
            Campaign closedCampaign = createTestCampaign(2L, "Closed meanwhile");
            closedCampaign.setGoal(closedGoal);

            givenRefreshLoads(completedGoal, CampaignStatus.COMPLETED, new BigDecimal("1000.00"));
            givenRefreshLoads(closedGoal, CampaignStatus.CLOSED, BigDecimal.ZERO);
            when(campaignRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of(completedCampaign, closedCampaign));

            // When
            campaignService.closeAllByOwner(TEST_USER_ID);

            // Then
            assertThat(completedGoal.getStatus()).isEqualTo(CampaignStatus.CLOSED);
            InOrder inOrder = inOrder(entityManager, campaignStatusHistoryService);
            inOrder.verify(entityManager).refresh(completedGoal, LockModeType.PESSIMISTIC_WRITE);
            inOrder.verify(campaignStatusHistoryService)
                    .recordTransition(completedCampaign, CampaignStatus.COMPLETED, CampaignStatus.CLOSED, TEST_USER_ID);
            verify(entityManager).refresh(closedGoal, LockModeType.PESSIMISTIC_WRITE);
            verify(campaignStatusHistoryService, never())
                    .recordTransition(eq(closedCampaign), any(), any(), any());
            verify(campaignRepository).saveAll(List.of(completedCampaign));
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
