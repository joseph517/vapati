package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.entity.*;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportActionService Tests")
class ReportActionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PublicationRepository publicationRepository;
    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private ReportActionService reportActionService;

    private Report testReport;
    private User testUser;
    private Publication testPublication;
    private Campaign testCampaign;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setActive(true);

        testPublication = new Publication();
        testPublication.setId(1L);

        testCampaign = new Campaign();
        testCampaign.setId(1L);

        testReport = new Report();
        testReport.setId(1L);
        testReport.setReportedEntityId(1L);
        testReport.setReportedEntityType(ReportedEntityType.USER);
    }

    @Nested
    @DisplayName("executeAction() tests")
    class ExecuteActionTests {

        @Test
        @DisplayName("Should do nothing when actionTaken is null")
        void executeAction_WithNullAction_ShouldDoNothing() {
            // Given
            testReport.setActionTaken(null);

            // When
            reportActionService.executeAction(testReport);

            // Then
            verifyNoInteractions(userRepository, publicationRepository, campaignRepository);
        }

        @Test
        @DisplayName("Should execute ban when actionTaken is USER_BANNED")
        void executeAction_WithUserBanned_ShouldExecuteBan() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_BANNED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            reportActionService.executeAction(testReport);

            // Then
            verify(userRepository).findById(1L);
            verify(userRepository).save(testUser);
            assertThat(testUser.getBanned()).isTrue();
        }

        @Test
        @DisplayName("Should execute suspension when actionTaken is USER_SUSPENDED")
        void executeAction_WithUserSuspended_ShouldExecuteSuspension() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_SUSPENDED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            reportActionService.executeAction(testReport);

            // Then
            verify(userRepository).findById(1L);
            verify(userRepository).save(testUser);
            assertThat(testUser.getSuspendedUntil()).isNotNull();
        }

        @Test
        @DisplayName("Should execute content removal when actionTaken is CONTENT_REMOVED")
        void executeAction_WithContentRemoved_ShouldRemoveContent() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.PUBLICATION);
            when(publicationRepository.findById(1L)).thenReturn(Optional.of(testPublication));

            // When
            reportActionService.executeAction(testReport);

            // Then
            verify(publicationRepository).findById(1L);
            verify(publicationRepository).delete(testPublication);
        }

        @Test
        @DisplayName("Should execute warning when actionTaken is WARNING_SENT")
        void executeAction_WithWarningSent_ShouldExecuteWarning() {
            // Given
            testReport.setActionTaken(ActionTaken.WARNING_SENT);

            // When & Then
            assertDoesNotThrow(() -> reportActionService.executeAction(testReport));
            verifyNoInteractions(userRepository, publicationRepository, campaignRepository);
        }

        @Test
        @DisplayName("Should do nothing when actionTaken is NO_ACTION")
        void executeAction_WithNoAction_ShouldDoNothing() {
            // Given
            testReport.setActionTaken(ActionTaken.NO_ACTION);

            // When
            reportActionService.executeAction(testReport);

            // Then
            verifyNoInteractions(userRepository, publicationRepository, campaignRepository);
        }

        @Test
        @DisplayName("Should do nothing when actionTaken is OTHER")
        void executeAction_WithOther_ShouldDoNothing() {
            // Given
            testReport.setActionTaken(ActionTaken.OTHER);

            // When
            reportActionService.executeAction(testReport);

            // Then
            verifyNoInteractions(userRepository, publicationRepository, campaignRepository);
        }
    }

    @Nested
    @DisplayName("executeBan() tests")
    class ExecuteBanTests {

        @Test
        @DisplayName("Should ban user successfully for USER entity type")
        void executeBan_WithUserEntityType_ShouldBanUser() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_BANNED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            testReport.setAdminNotes("Spam violation");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            reportActionService.executeAction(testReport);

            // Then
            assertThat(testUser.getBanned()).isTrue();
            assertThat(testUser.getBannedAt()).isCloseTo(LocalDateTime.now(), within(1, java.time.temporal.ChronoUnit.SECONDS));
            assertThat(testUser.getBannedReason()).isEqualTo("Spam violation");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw MessageException when user not found")
        void executeBan_WithNonExistentUser_ShouldThrowException() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_BANNED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportActionService.executeAction(testReport))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User not found");

            verify(userRepository).findById(1L);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set ban reason from admin notes when provided")
        void executeBan_WithAdminNotes_ShouldSetCustomReason() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_BANNED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            testReport.setAdminNotes("Spam violation");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            reportActionService.executeAction(testReport);

            // Then
            assertThat(testUser.getBannedReason()).isEqualTo("Spam violation");
        }

        @Test
        @DisplayName("Should use default ban reason when admin notes null")
        void executeBan_WithoutAdminNotes_ShouldUseDefaultReason() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_BANNED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            testReport.setAdminNotes(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            reportActionService.executeAction(testReport);

            // Then
            assertThat(testUser.getBannedReason()).isEqualTo("Banned by admin");
        }

        @Test
        @DisplayName("Should set bannedAt timestamp")
        void executeBan_ShouldSetBannedAtTimestamp() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_BANNED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            LocalDateTime beforeBan = LocalDateTime.now();

            // When
            reportActionService.executeAction(testReport);

            // Then
            assertThat(testUser.getBannedAt()).isNotNull();
            assertThat(testUser.getBannedAt()).isAfterOrEqualTo(beforeBan);
        }

        @Test
        @DisplayName("Should not ban for non-USER entity types")
        void executeBan_WithNonUserEntityType_ShouldDoNothing() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_BANNED);
            testReport.setReportedEntityType(ReportedEntityType.PUBLICATION);

            // When
            reportActionService.executeAction(testReport);

            // Then
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("executeSuspension() tests")
    class ExecuteSuspensionTests {

        @Test
        @DisplayName("Should suspend user for 30 days successfully")
        void executeSuspension_WithUserEntityType_ShouldSuspendUser() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_SUSPENDED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            testReport.setAdminNotes("Harassment");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            reportActionService.executeAction(testReport);

            // Then
            assertThat(testUser.getSuspendedUntil()).isNotNull();
            assertThat(testUser.getBannedReason()).isEqualTo("Harassment");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw MessageException when user not found")
        void executeSuspension_WithNonExistentUser_ShouldThrowException() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_SUSPENDED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportActionService.executeAction(testReport))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User not found");

            verify(userRepository).findById(1L);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set suspension reason from admin notes when provided")
        void executeSuspension_WithAdminNotes_ShouldSetCustomReason() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_SUSPENDED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            testReport.setAdminNotes("Harassment");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            reportActionService.executeAction(testReport);

            // Then
            assertThat(testUser.getBannedReason()).isEqualTo("Harassment");
        }

        @Test
        @DisplayName("Should use default suspension reason when admin notes null")
        void executeSuspension_WithoutAdminNotes_ShouldUseDefaultReason() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_SUSPENDED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            testReport.setAdminNotes(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            reportActionService.executeAction(testReport);

            // Then
            assertThat(testUser.getBannedReason()).isEqualTo("Suspended by admin");
        }

        @Test
        @DisplayName("Should set suspendedUntil to exactly 30 days from now")
        void executeSuspension_ShouldSetSuspensionFor30Days() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_SUSPENDED);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            LocalDateTime expectedSuspensionEnd = LocalDateTime.now().plusDays(30);

            // When
            reportActionService.executeAction(testReport);

            // Then
            assertThat(testUser.getSuspendedUntil())
                    .isCloseTo(expectedSuspensionEnd, within(5, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Should not suspend for non-USER entity types")
        void executeSuspension_WithNonUserEntityType_ShouldDoNothing() {
            // Given
            testReport.setActionTaken(ActionTaken.USER_SUSPENDED);
            testReport.setReportedEntityType(ReportedEntityType.CAMPAIGN);

            // When
            reportActionService.executeAction(testReport);

            // Then
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("executeContentRemoval() tests")
    class ExecuteContentRemovalTests {

        @Test
        @DisplayName("Should route to removePublication for PUBLICATION entity type")
        void executeContentRemoval_WithPublication_ShouldRemovePublication() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.PUBLICATION);
            when(publicationRepository.findById(1L)).thenReturn(Optional.of(testPublication));

            // When
            reportActionService.executeAction(testReport);

            // Then
            verify(publicationRepository).findById(1L);
            verify(publicationRepository).delete(testPublication);
        }

        @Test
        @DisplayName("Should route to removeCampaign for CAMPAIGN entity type")
        void executeContentRemoval_WithCampaign_ShouldRemoveCampaign() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.CAMPAIGN);
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));

            // When
            reportActionService.executeAction(testReport);

            // Then
            verify(campaignRepository).findById(1L);
            verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("Should do nothing for USER entity type")
        void executeContentRemoval_WithUser_ShouldDoNothing() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.USER);

            // When
            reportActionService.executeAction(testReport);

            // Then
            verifyNoInteractions(userRepository, publicationRepository, campaignRepository);
        }

        @Test
        @DisplayName("Should handle unknown entity types gracefully")
        void executeContentRemoval_WithUnknownType_ShouldHandleGracefully() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.USER);

            // When & Then
            assertDoesNotThrow(() -> reportActionService.executeAction(testReport));
        }
    }

    @Nested
    @DisplayName("executeWarning() tests")
    class ExecuteWarningTests {

        @Test
        @DisplayName("Should execute without errors")
        void executeWarning_ShouldExecuteWithoutError() {
            // Given
            testReport.setActionTaken(ActionTaken.WARNING_SENT);

            // When & Then
            assertDoesNotThrow(() -> reportActionService.executeAction(testReport));
        }

        @Test
        @DisplayName("Should not throw exception for any report")
        void executeWarning_WithAnyReport_ShouldNotThrowException() {
            // Given
            testReport.setActionTaken(ActionTaken.WARNING_SENT);
            testReport.setReportedEntityType(ReportedEntityType.USER);
            testReport.setAdminNotes("Warning message");

            // When & Then
            assertDoesNotThrow(() -> reportActionService.executeAction(testReport));
        }
    }

    @Nested
    @DisplayName("removePublication() tests")
    class RemovePublicationTests {

        @Test
        @DisplayName("Should soft delete publication successfully")
        void removePublication_WithValidId_ShouldDeletePublication() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.PUBLICATION);
            when(publicationRepository.findById(1L)).thenReturn(Optional.of(testPublication));

            // When
            reportActionService.executeAction(testReport);

            // Then
            verify(publicationRepository).findById(1L);
            verify(publicationRepository).delete(testPublication);
        }

        @Test
        @DisplayName("Should throw MessageException when publication not found")
        void removePublication_WithInvalidId_ShouldThrowException() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.PUBLICATION);
            when(publicationRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportActionService.executeAction(testReport))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Publication not found");

            verify(publicationRepository).findById(1L);
            verify(publicationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should rely on @SQLDelete annotation for soft delete")
        void removePublication_ShouldUseSoftDelete() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.PUBLICATION);
            when(publicationRepository.findById(1L)).thenReturn(Optional.of(testPublication));

            // When
            reportActionService.executeAction(testReport);

            // Then
            verify(publicationRepository).delete(testPublication);
        }
    }

    @Nested
    @DisplayName("removeCampaign() tests")
    class RemoveCampaignTests {

        @Test
        @DisplayName("Should soft delete campaign successfully")
        void removeCampaign_WithValidId_ShouldDeleteCampaign() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.CAMPAIGN);
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));

            // When
            reportActionService.executeAction(testReport);

            // Then
            verify(campaignRepository).findById(1L);
            verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("Should throw MessageException when campaign not found")
        void removeCampaign_WithInvalidId_ShouldThrowException() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.CAMPAIGN);
            when(campaignRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportActionService.executeAction(testReport))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign not found");

            verify(campaignRepository).findById(1L);
            verify(campaignRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should rely on @SQLDelete annotation for soft delete")
        void removeCampaign_ShouldUseSoftDelete() {
            // Given
            testReport.setActionTaken(ActionTaken.CONTENT_REMOVED);
            testReport.setReportedEntityType(ReportedEntityType.CAMPAIGN);
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));

            // When
            reportActionService.executeAction(testReport);

            // Then
            verify(campaignRepository).delete(testCampaign);
        }
    }
}
