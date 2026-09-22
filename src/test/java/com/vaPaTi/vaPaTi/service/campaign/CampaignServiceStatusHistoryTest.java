package com.vaPaTi.vaPaTi.service.campaign;

import com.vaPaTi.vaPaTi.dtos.CampaignStatusHistoryResponseDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.CampaignStatusHistory;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.CampaignStatusHistoryMapper;
import com.vaPaTi.vaPaTi.repository.CampaignCategoryRepository;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.CampaignStatusHistoryRepository;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.CampaignService;
import com.vaPaTi.vaPaTi.service.CampaignStatusHistoryService;
import com.vaPaTi.vaPaTi.validation.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.CampaignServiceValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignService status history Tests")
class CampaignServiceStatusHistoryTest {

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

    @InjectMocks
    private CampaignService campaignService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_CAMPAIGN_ID = 1L;

    private User testUser;
    private Campaign testCampaign;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(TEST_USER_ID);

        testCampaign = new Campaign();
        testCampaign.setId(TEST_CAMPAIGN_ID);
        testCampaign.setUser(testUser);
    }

    @Test
    @DisplayName("Should return the campaign's status history ordered chronologically")
    void getCampaignStatusHistory_WithAuthorizedUser_ShouldReturnMappedDTOs() {
        // Given
        CampaignStatusHistory entry1 = CampaignStatusHistory.builder()
                .campaign(testCampaign)
                .previousStatus(null)
                .newStatus(CampaignStatus.ACTIVE)
                .changedBy(testUser)
                .build();
        CampaignStatusHistory entry2 = CampaignStatusHistory.builder()
                .campaign(testCampaign)
                .previousStatus(CampaignStatus.ACTIVE)
                .newStatus(CampaignStatus.CLOSED)
                .changedBy(testUser)
                .build();

        CampaignStatusHistoryResponseDTO dto1 =
                new CampaignStatusHistoryResponseDTO(null, CampaignStatus.ACTIVE, TEST_USER_ID, null);
        CampaignStatusHistoryResponseDTO dto2 =
                new CampaignStatusHistoryResponseDTO(CampaignStatus.ACTIVE, CampaignStatus.CLOSED, TEST_USER_ID, null);

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
        doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
        when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
        when(campaignStatusHistoryRepository.findByCampaignIdOrderByChangedAtAsc(TEST_CAMPAIGN_ID))
                .thenReturn(List.of(entry1, entry2));

        try (MockedStatic<CampaignStatusHistoryMapper> mapperMock = mockStatic(CampaignStatusHistoryMapper.class)) {
            mapperMock.when(() -> CampaignStatusHistoryMapper.toResponseDTO(entry1)).thenReturn(dto1);
            mapperMock.when(() -> CampaignStatusHistoryMapper.toResponseDTO(entry2)).thenReturn(dto2);

            // When
            List<CampaignStatusHistoryResponseDTO> result = campaignService.getCampaignStatusHistory(TEST_CAMPAIGN_ID);

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
            verify(campaignServiceValidation).findCampaignByIdOrThrow(TEST_CAMPAIGN_ID);
            verify(campaignStatusHistoryRepository).findByCampaignIdOrderByChangedAtAsc(TEST_CAMPAIGN_ID);
        }
    }

    @Test
    @DisplayName("Should return an empty list when the campaign has no history entries")
    void getCampaignStatusHistory_WithNoEntries_ShouldReturnEmptyList() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
        doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
        when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID)).thenReturn(testCampaign);
        when(campaignStatusHistoryRepository.findByCampaignIdOrderByChangedAtAsc(TEST_CAMPAIGN_ID))
                .thenReturn(List.of());

        // When
        List<CampaignStatusHistoryResponseDTO> result = campaignService.getCampaignStatusHistory(TEST_CAMPAIGN_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw MessageException when the user is not owner nor admin")
    void getCampaignStatusHistory_WhenNotAuthorized_ShouldThrowException() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
        doThrow(new MessageException("You are not authorized to perform this action"))
                .when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);

        // When & Then
        assertThatThrownBy(() -> campaignService.getCampaignStatusHistory(TEST_CAMPAIGN_ID))
                .isInstanceOf(MessageException.class)
                .hasMessage("You are not authorized to perform this action");

        verify(campaignStatusHistoryRepository, never()).findByCampaignIdOrderByChangedAtAsc(any());
    }

    @Test
    @DisplayName("Should throw MessageException when the campaign does not exist")
    void getCampaignStatusHistory_WithNonExistentCampaign_ShouldThrowException() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
        doNothing().when(campaignAuthorizationService).validateOwnershipOrAdmin(TEST_CAMPAIGN_ID, TEST_USER_ID);
        when(campaignServiceValidation.findCampaignByIdOrThrow(TEST_CAMPAIGN_ID))
                .thenThrow(new MessageException("Campaign not found with id: " + TEST_CAMPAIGN_ID));

        // When & Then
        assertThatThrownBy(() -> campaignService.getCampaignStatusHistory(TEST_CAMPAIGN_ID))
                .isInstanceOf(MessageException.class)
                .hasMessage("Campaign not found with id: " + TEST_CAMPAIGN_ID);

        verify(campaignStatusHistoryRepository, never()).findByCampaignIdOrderByChangedAtAsc(any());
    }

    @Nested
    @DisplayName("closeAndSoftDelete() tests")
    class CloseAndSoftDeleteTests {

        private static final Long CHANGED_BY_USER_ID = 99L;

        private Goal goalWithStatus(CampaignStatus status) {
            Goal goal = new Goal();
            goal.setId(1L);
            goal.setStatus(status);
            testCampaign.setGoal(goal);
            return goal;
        }

        @Test
        @DisplayName("Should close an ACTIVE goal, record ACTIVE -> CLOSED with the given user and soft delete")
        void closeAndSoftDelete_WithActiveGoal_ShouldCloseRecordAndDelete() {
            // Given
            Goal goal = goalWithStatus(CampaignStatus.ACTIVE);

            // When
            campaignService.closeAndSoftDelete(testCampaign, CHANGED_BY_USER_ID);

            // Then
            assertThat(goal.getStatus()).isEqualTo(CampaignStatus.CLOSED);
            verify(campaignStatusHistoryService)
                    .recordTransition(testCampaign, CampaignStatus.ACTIVE, CampaignStatus.CLOSED, CHANGED_BY_USER_ID);
            InOrder inOrder = inOrder(campaignRepository);
            inOrder.verify(campaignRepository).saveAndFlush(testCampaign);
            inOrder.verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("Should close a COMPLETED goal, record COMPLETED -> CLOSED and soft delete")
        void closeAndSoftDelete_WithCompletedGoal_ShouldCloseRecordAndDelete() {
            // Given
            Goal goal = goalWithStatus(CampaignStatus.COMPLETED);

            // When
            campaignService.closeAndSoftDelete(testCampaign, CHANGED_BY_USER_ID);

            // Then
            assertThat(goal.getStatus()).isEqualTo(CampaignStatus.CLOSED);
            verify(campaignStatusHistoryService)
                    .recordTransition(testCampaign, CampaignStatus.COMPLETED, CampaignStatus.CLOSED, CHANGED_BY_USER_ID);
            InOrder inOrder = inOrder(campaignRepository);
            inOrder.verify(campaignRepository).saveAndFlush(testCampaign);
            inOrder.verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("Should not record a transition when the goal is already CLOSED, but still soft delete")
        void closeAndSoftDelete_WithClosedGoal_ShouldOnlyDelete() {
            // Given
            Goal goal = goalWithStatus(CampaignStatus.CLOSED);

            // When
            campaignService.closeAndSoftDelete(testCampaign, CHANGED_BY_USER_ID);

            // Then
            assertThat(goal.getStatus()).isEqualTo(CampaignStatus.CLOSED);
            verify(campaignStatusHistoryService, never()).recordTransition(any(), any(), any(), any());
            verify(campaignRepository, never()).saveAndFlush(any());
            verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("Should soft delete without recording a transition when the campaign has no goal")
        void closeAndSoftDelete_WithoutGoal_ShouldOnlyDelete() {
            // Given
            testCampaign.setGoal(null);

            // When
            campaignService.closeAndSoftDelete(testCampaign, CHANGED_BY_USER_ID);

            // Then
            verify(campaignStatusHistoryService, never()).recordTransition(any(), any(), any(), any());
            verify(campaignRepository, never()).saveAndFlush(any());
            verify(campaignRepository).delete(testCampaign);
        }
    }
}
