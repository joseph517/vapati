package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.CampaignStatusHistory;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.repository.CampaignStatusHistoryRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignStatusHistoryService Tests")
class CampaignStatusHistoryServiceTest {

    @Mock
    private CampaignStatusHistoryRepository campaignStatusHistoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CampaignStatusHistoryService campaignStatusHistoryService;

    private static final Long TEST_USER_ID = 1L;

    private Campaign testCampaign;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(TEST_USER_ID);

        testCampaign = new Campaign();
        testCampaign.setId(1L);
    }

    @Test
    @DisplayName("Should save an entry resolving changedByUserId to a User when not null")
    void recordTransition_WithChangedByUserId_ShouldResolveAndSaveUser() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        ArgumentCaptor<CampaignStatusHistory> captor = ArgumentCaptor.forClass(CampaignStatusHistory.class);

        // When
        campaignStatusHistoryService.recordTransition(testCampaign, null, CampaignStatus.ACTIVE, TEST_USER_ID);

        // Then
        verify(campaignStatusHistoryRepository).save(captor.capture());
        CampaignStatusHistory saved = captor.getValue();
        assertThat(saved.getCampaign()).isEqualTo(testCampaign);
        assertThat(saved.getPreviousStatus()).isNull();
        assertThat(saved.getNewStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(saved.getChangedBy()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should save an entry with a null changedBy when changedByUserId is null")
    void recordTransition_WithNullChangedByUserId_ShouldSaveWithNullChangedBy() {
        // Given
        ArgumentCaptor<CampaignStatusHistory> captor = ArgumentCaptor.forClass(CampaignStatusHistory.class);

        // When
        campaignStatusHistoryService.recordTransition(testCampaign, CampaignStatus.ACTIVE, CampaignStatus.COMPLETED, null);

        // Then
        verify(campaignStatusHistoryRepository).save(captor.capture());
        CampaignStatusHistory saved = captor.getValue();
        assertThat(saved.getChangedBy()).isNull();
        assertThat(saved.getPreviousStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(saved.getNewStatus()).isEqualTo(CampaignStatus.COMPLETED);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should save an entry with a null changedBy when the referenced user does not exist")
    void recordTransition_WithNonExistentUser_ShouldSaveWithNullChangedBy() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        ArgumentCaptor<CampaignStatusHistory> captor = ArgumentCaptor.forClass(CampaignStatusHistory.class);

        // When
        campaignStatusHistoryService.recordTransition(testCampaign, CampaignStatus.CLOSED, CampaignStatus.ACTIVE, TEST_USER_ID);

        // Then
        verify(campaignStatusHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedBy()).isNull();
    }
}
