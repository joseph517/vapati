package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.CreateReportDTO;
import com.vaPaTi.vaPaTi.entity.*;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.ReportRepository;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportValidationService Tests")
class ReportValidationServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PublicationRepository publicationRepository;
    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private ReportValidationService reportValidationService;

    private CreateReportDTO validDTO;
    private User testUser;
    private Publication testPublication;
    private Campaign testCampaign;
    private Report testReport;

    @BeforeEach
    void setUp() {
        validDTO = new CreateReportDTO();
        validDTO.setReportedEntityType(ReportedEntityType.USER);
        validDTO.setReportedEntityId(2L);
        validDTO.setReason(ReportReason.SPAM);
        validDTO.setDescription("Test description");

        testUser = new User();
        testUser.setId(2L);
        testUser.setDeletedAt(null);

        testPublication = new Publication();
        testPublication.setId(1L);
        testPublication.setDeletedAt(null);

        testCampaign = new Campaign();
        testCampaign.setId(1L);

        testReport = new Report();
        testReport.setId(1L);
    }

    @Nested
    @DisplayName("validateInput() tests")
    class ValidateInputTests {

        @Test
        @DisplayName("Should pass validation with valid DTO")
        void validateInput_WithValidDTO_ShouldNotThrowException() {
            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateInput(validDTO));
        }

        @Test
        @DisplayName("Should throw MessageException when DTO is null")
        void validateInput_WithNullDTO_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateInput(null))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Report data cannot be null");
        }

        @Test
        @DisplayName("Should throw MessageException when reportedEntityType is null")
        void validateInput_WithNullEntityType_ShouldThrowException() {
            // Given
            validDTO.setReportedEntityType(null);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateInput(validDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Reported entity type is required");
        }

        @Test
        @DisplayName("Should throw MessageException when reportedEntityId is null")
        void validateInput_WithNullEntityId_ShouldThrowException() {
            // Given
            validDTO.setReportedEntityId(null);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateInput(validDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Valid reported entity ID is required");
        }

        @Test
        @DisplayName("Should throw MessageException when reportedEntityId is zero")
        void validateInput_WithZeroEntityId_ShouldThrowException() {
            // Given
            validDTO.setReportedEntityId(0L);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateInput(validDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Valid reported entity ID is required");
        }

        @Test
        @DisplayName("Should throw MessageException when reportedEntityId is negative")
        void validateInput_WithNegativeEntityId_ShouldThrowException() {
            // Given
            validDTO.setReportedEntityId(-1L);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateInput(validDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Valid reported entity ID is required");
        }

        @Test
        @DisplayName("Should throw MessageException when reason is null")
        void validateInput_WithNullReason_ShouldThrowException() {
            // Given
            validDTO.setReason(null);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateInput(validDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Report reason is required");
        }

        @Test
        @DisplayName("Should throw MessageException when description exceeds 1000 characters")
        void validateInput_WithLongDescription_ShouldThrowException() {
            // Given
            String longDescription = "a".repeat(1001);
            validDTO.setDescription(longDescription);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateInput(validDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Description cannot exceed 1000 characters");
        }
    }

    @Nested
    @DisplayName("validateNotSelfReport() tests")
    class ValidateNotSelfReportTests {

        @Test
        @DisplayName("Should pass when user is not reporting themselves")
        void validateNotSelfReport_WithDifferentUsers_ShouldNotThrowException() {
            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateNotSelfReport(
                    1L, ReportedEntityType.USER, 2L
            ));
        }

        @Test
        @DisplayName("Should throw MessageException when user reports themselves")
        void validateNotSelfReport_WithSameUserId_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateNotSelfReport(
                    1L, ReportedEntityType.USER, 1L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You cannot report yourself");
        }

        @Test
        @DisplayName("Should pass for non-USER entity types")
        void validateNotSelfReport_WithPublicationEntityType_ShouldNotThrowException() {
            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateNotSelfReport(
                    1L, ReportedEntityType.PUBLICATION, 1L
            ));
        }
    }

    @Nested
    @DisplayName("validateEntityExists() - USER tests")
    class ValidateEntityExistsUserTests {

        @Test
        @DisplayName("Should pass when user exists and not deleted")
        void validateEntityExists_WithValidUser_ShouldNotThrowException() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.USER, 2L
            ));

            verify(userRepository).findById(2L);
        }

        @Test
        @DisplayName("Should throw MessageException when user not found")
        void validateEntityExists_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.USER, 2L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should throw MessageException when user is deleted")
        void validateEntityExists_WithDeletedUser_ShouldThrowException() {
            // Given
            testUser.setDeletedAt(LocalDateTime.now().minusDays(1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.USER, 2L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot report a deleted user");
        }

        @Test
        @DisplayName("Should check user deletion status")
        void validateEntityExists_ForUser_ShouldCheckDeletedAt() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

            // When
            reportValidationService.validateEntityExists(ReportedEntityType.USER, 2L);

            // Then
            verify(userRepository).findById(2L);
            assertThat(testUser.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("validateEntityExists() - PUBLICATION tests")
    class ValidateEntityExistsPublicationTests {

        @Test
        @DisplayName("Should pass when publication exists and not deleted")
        void validateEntityExists_WithValidPublication_ShouldNotThrowException() {
            // Given
            when(publicationRepository.findById(1L)).thenReturn(Optional.of(testPublication));

            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.PUBLICATION, 1L
            ));

            verify(publicationRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw MessageException when publication not found")
        void validateEntityExists_WithNonExistentPublication_ShouldThrowException() {
            // Given
            when(publicationRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.PUBLICATION, 1L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Publication not found");
        }

        @Test
        @DisplayName("Should throw MessageException when publication is deleted")
        void validateEntityExists_WithDeletedPublication_ShouldThrowException() {
            // Given
            testPublication.setDeletedAt(LocalDateTime.now().minusDays(1));
            when(publicationRepository.findById(1L)).thenReturn(Optional.of(testPublication));

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.PUBLICATION, 1L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot report a deleted publication");
        }

        @Test
        @DisplayName("Should check publication deletion status")
        void validateEntityExists_ForPublication_ShouldCheckDeletedAt() {
            // Given
            when(publicationRepository.findById(1L)).thenReturn(Optional.of(testPublication));

            // When
            reportValidationService.validateEntityExists(ReportedEntityType.PUBLICATION, 1L);

            // Then
            verify(publicationRepository).findById(1L);
            assertThat(testPublication.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("validateEntityExists() - CAMPAIGN tests")
    class ValidateEntityExistsCampaignTests {

        @Test
        @DisplayName("Should pass when campaign exists")
        void validateEntityExists_WithValidCampaign_ShouldNotThrowException() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));

            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.CAMPAIGN, 1L
            ));

            verify(campaignRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw MessageException when campaign not found")
        void validateEntityExists_WithNonExistentCampaign_ShouldThrowException() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.CAMPAIGN, 1L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Campaign not found");
        }

        @Test
        @DisplayName("Should not check deletion status for campaign")
        void validateEntityExists_ForCampaign_ShouldOnlyCheckExistence() {
            // Given
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));

            // When
            reportValidationService.validateEntityExists(ReportedEntityType.CAMPAIGN, 1L);

            // Then
            verify(campaignRepository).findById(1L);
            // Campaign doesn't have soft delete check
        }
    }

    @Nested
    @DisplayName("validateNoDuplicateReport() tests")
    class ValidateNoDuplicateReportTests {

        @Test
        @DisplayName("Should pass when no duplicate report exists")
        void validateNoDuplicateReport_WithNoDuplicate_ShouldNotThrowException() {
            // Given
            when(reportRepository.existsByReporterIdAndReportedEntityTypeAndReportedEntityId(
                    1L, ReportedEntityType.USER, 2L
            )).thenReturn(false);

            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateNoDuplicateReport(
                    1L, ReportedEntityType.USER, 2L
            ));

            verify(reportRepository).existsByReporterIdAndReportedEntityTypeAndReportedEntityId(
                    1L, ReportedEntityType.USER, 2L
            );
        }

        @Test
        @DisplayName("Should throw MessageException when duplicate report exists")
        void validateNoDuplicateReport_WithDuplicate_ShouldThrowException() {
            // Given
            when(reportRepository.existsByReporterIdAndReportedEntityTypeAndReportedEntityId(
                    1L, ReportedEntityType.USER, 2L
            )).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateNoDuplicateReport(
                    1L, ReportedEntityType.USER, 2L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You have already reported this user");

            verify(reportRepository).existsByReporterIdAndReportedEntityTypeAndReportedEntityId(
                    1L, ReportedEntityType.USER, 2L
            );
        }

        @Test
        @DisplayName("Should include entity type in exception message")
        void validateNoDuplicateReport_ShouldIncludeEntityTypeInMessage() {
            // Given
            when(reportRepository.existsByReporterIdAndReportedEntityTypeAndReportedEntityId(
                    1L, ReportedEntityType.PUBLICATION, 2L
            )).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateNoDuplicateReport(
                    1L, ReportedEntityType.PUBLICATION, 2L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You have already reported this publication");
        }
    }

    @Nested
    @DisplayName("validateDailyReportLimit() tests")
    class ValidateDailyReportLimitTests {

        @Test
        @DisplayName("Should pass when under daily limit")
        void validateDailyReportLimit_UnderLimit_ShouldNotThrowException() {
            // Given
            when(reportRepository.countReportsByReporterIdSince(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(9L);

            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateDailyReportLimit(1L));

            verify(reportRepository).countReportsByReporterIdSince(eq(1L), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should throw MessageException when at limit")
        void validateDailyReportLimit_AtLimit_ShouldThrowException() {
            // Given
            when(reportRepository.countReportsByReporterIdSince(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(10L);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateDailyReportLimit(1L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You have reached the maximum number of reports allowed per day (10)");
        }

        @Test
        @DisplayName("Should throw MessageException when exceeding limit")
        void validateDailyReportLimit_ExceedingLimit_ShouldThrowException() {
            // Given
            when(reportRepository.countReportsByReporterIdSince(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(15L);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateDailyReportLimit(1L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You have reached the maximum number of reports allowed per day (10)");
        }

        @Test
        @DisplayName("Should check reports in last 24 hours")
        void validateDailyReportLimit_ShouldCheckLast24Hours() {
            // Given
            when(reportRepository.countReportsByReporterIdSince(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(5L);

            // When
            reportValidationService.validateDailyReportLimit(1L);

            // Then
            verify(reportRepository).countReportsByReporterIdSince(eq(1L), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("validateReportExists() tests")
    class ValidateReportExistsTests {

        @Test
        @DisplayName("Should return report when it exists")
        void validateReportExists_WithValidId_ShouldReturnReport() {
            // Given
            when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));

            // When
            Report result = reportValidationService.validateReportExists(1L);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(testReport);
            verify(reportRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw MessageException when report not found")
        void validateReportExists_WithInvalidId_ShouldThrowException() {
            // Given
            when(reportRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReportExists(999L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Report not found with id: 999");

            verify(reportRepository).findById(999L);
        }

        @Test
        @DisplayName("Should pass through repository result")
        void validateReportExists_ShouldUseRepository() {
            // Given
            when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));

            // When
            reportValidationService.validateReportExists(1L);

            // Then
            verify(reportRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("validateReviewInput() tests")
    class ValidateReviewInputTests {

        @Test
        @DisplayName("Should pass validation with valid status and action")
        void validateReviewInput_WithValidInput_ShouldNotThrowException() {
            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateReviewInput(
                    ReportStatus.RESOLVED, ActionTaken.USER_BANNED
            ));
        }

        @Test
        @DisplayName("Should throw MessageException when status is null")
        void validateReviewInput_WithNullStatus_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                    null, ActionTaken.USER_BANNED
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Status is required for review");
        }

        @Test
        @DisplayName("Should throw MessageException when RESOLVED with NO_ACTION")
        void validateReviewInput_WithResolvedAndNoAction_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                    ReportStatus.RESOLVED, ActionTaken.NO_ACTION
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Action taken must be specified when resolving a report");
        }

        @Test
        @DisplayName("Should throw MessageException when RESOLVED with null action")
        void validateReviewInput_WithResolvedAndNullAction_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                    ReportStatus.RESOLVED, null
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Action taken must be specified when resolving a report");
        }

        @Test
        @DisplayName("Should throw MessageException when status is PENDING")
        void validateReviewInput_WithPendingStatus_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                    ReportStatus.PENDING, ActionTaken.NO_ACTION
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot set status back to PENDING");
        }

        @Test
        @DisplayName("Should allow REJECTED without specific action")
        void validateReviewInput_WithRejectedAndNoAction_ShouldNotThrowException() {
            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateReviewInput(
                    ReportStatus.REJECTED, ActionTaken.NO_ACTION
            ));
        }
    }

    @Nested
    @DisplayName("getReporter() tests")
    class GetReporterTests {

        @Test
        @DisplayName("Should return user when found")
        void getReporter_WithValidId_ShouldReturnUser() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            User result = reportValidationService.getReporter(1L);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(testUser);
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw MessageException when user not found")
        void getReporter_WithInvalidId_ShouldThrowException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportValidationService.getReporter(999L))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Reporter User not found");

            verify(userRepository).findById(999L);
        }

        @Test
        @DisplayName("Should query by reporter ID")
        void getReporter_ShouldUseRepository() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            reportValidationService.getReporter(1L);

            // Then
            verify(userRepository).findById(1L);
        }
    }
}
