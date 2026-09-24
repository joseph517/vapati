package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.CreateReportDTO;
import com.vaPaTi.vaPaTi.entity.*;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.ReportRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportValidationService Tests")
class ReportValidationServiceTest {

    private static final Long REPORTER_ID = 1L;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PublicationRepository publicationRepository;
    @Mock
    private CampaignServiceValidation campaignServiceValidation;

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
        @DisplayName("Should throw MessageException when user reports their own publication")
        void validateNotSelfReport_WithOwnPublication_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateNotSelfReport(
                    1L, ReportedEntityType.PUBLICATION, 1L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You cannot report your own publication");
        }

        @Test
        @DisplayName("Should throw MessageException when user reports their own campaign")
        void validateNotSelfReport_WithOwnCampaign_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateNotSelfReport(
                    1L, ReportedEntityType.CAMPAIGN, 1L
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You cannot report your own campaign");
        }

        @Test
        @DisplayName("Should pass when the publication or campaign belongs to someone else")
        void validateNotSelfReport_WithOtherOwner_ShouldNotThrowException() {
            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateNotSelfReport(
                    1L, ReportedEntityType.PUBLICATION, 2L
            ));
            assertDoesNotThrow(() -> reportValidationService.validateNotSelfReport(
                    1L, ReportedEntityType.CAMPAIGN, 2L
            ));
        }

        @Test
        @DisplayName("Should pass when the owner is null (deleted author)")
        void validateNotSelfReport_WithNullOwner_ShouldNotThrowException() {
            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateNotSelfReport(
                    1L, ReportedEntityType.PUBLICATION, null
            ));
            assertDoesNotThrow(() -> reportValidationService.validateNotSelfReport(
                    1L, ReportedEntityType.CAMPAIGN, null
            ));
        }
    }

    @Nested
    @DisplayName("validateEntityExists() - owner tests")
    class ValidateEntityExistsOwnerTests {

        @Test
        @DisplayName("Should return the reported user id as the owner of a USER")
        void validateEntityExists_ForUser_ShouldReturnEntityIdAsOwner() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThat(reportValidationService.validateEntityExists(ReportedEntityType.USER, 2L, REPORTER_ID))
                    .isEqualTo(2L);
        }

        @Test
        @DisplayName("Should return the author id as the owner of a publication")
        void validateEntityExists_ForPublication_ShouldReturnAuthorId() {
            // Given
            User author = new User();
            author.setId(5L);
            testPublication.setUser(author);
            when(publicationRepository.findById(1L)).thenReturn(Optional.of(testPublication));

            // When & Then
            assertThat(reportValidationService.validateEntityExists(ReportedEntityType.PUBLICATION, 1L, REPORTER_ID))
                    .isEqualTo(5L);
        }

        @Test
        @DisplayName("Should return null as the owner of a publication whose author is deleted")
        void validateEntityExists_ForPublicationWithDeletedAuthor_ShouldReturnNull() {
            // Given: a soft-deleted author is loaded as null
            testPublication.setUser(null);
            when(publicationRepository.findById(1L)).thenReturn(Optional.of(testPublication));

            // When & Then
            assertThat(reportValidationService.validateEntityExists(ReportedEntityType.PUBLICATION, 1L, REPORTER_ID))
                    .isNull();
        }

        @Test
        @DisplayName("Should return the campaign owner id")
        void validateEntityExists_ForCampaign_ShouldReturnOwnerId() {
            // Given
            User owner = new User();
            owner.setId(6L);
            testCampaign.setUser(owner);
            when(campaignServiceValidation.findVisibleCampaignByIdOrThrow(1L, REPORTER_ID)).thenReturn(testCampaign);

            // When & Then
            assertThat(reportValidationService.validateEntityExists(ReportedEntityType.CAMPAIGN, 1L, REPORTER_ID))
                    .isEqualTo(6L);
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
                    ReportedEntityType.USER, 2L, REPORTER_ID
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
                    ReportedEntityType.USER, 2L, REPORTER_ID
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
                    ReportedEntityType.USER, 2L, REPORTER_ID
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
            reportValidationService.validateEntityExists(ReportedEntityType.USER, 2L, REPORTER_ID);

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
                    ReportedEntityType.PUBLICATION, 1L, REPORTER_ID
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
                    ReportedEntityType.PUBLICATION, 1L, REPORTER_ID
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
                    ReportedEntityType.PUBLICATION, 1L, REPORTER_ID
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
            reportValidationService.validateEntityExists(ReportedEntityType.PUBLICATION, 1L, REPORTER_ID);

            // Then
            verify(publicationRepository).findById(1L);
            assertThat(testPublication.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("validateEntityExists() - CAMPAIGN tests")
    class ValidateEntityExistsCampaignTests {

        @Test
        @DisplayName("Should pass when the campaign is visible to the reporter")
        void validateEntityExists_WithVisibleCampaign_ShouldNotThrowException() {
            // Given
            when(campaignServiceValidation.findVisibleCampaignByIdOrThrow(1L, REPORTER_ID)).thenReturn(testCampaign);

            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.CAMPAIGN, 1L, REPORTER_ID
            ));

            verify(campaignServiceValidation).findVisibleCampaignByIdOrThrow(1L, REPORTER_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the campaign does not exist or is not visible")
        void validateEntityExists_WithNonVisibleCampaign_ShouldThrowResourceNotFoundException() {
            // Given: a missing id, a deleted owner and a CLOSED campaign of someone else all end here
            when(campaignServiceValidation.findVisibleCampaignByIdOrThrow(1L, REPORTER_ID))
                    .thenThrow(new ResourceNotFoundException("Campaign not found with id: 1"));

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.CAMPAIGN, 1L, REPORTER_ID
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Campaign not found with id: 1");
        }

        @Test
        @DisplayName("Should not query the other repositories for a campaign")
        void validateEntityExists_ForCampaign_ShouldOnlyUseVisibleLookup() {
            // Given
            when(campaignServiceValidation.findVisibleCampaignByIdOrThrow(1L, REPORTER_ID)).thenReturn(testCampaign);

            // When
            reportValidationService.validateEntityExists(ReportedEntityType.CAMPAIGN, 1L, REPORTER_ID);

            // Then
            verifyNoInteractions(userRepository, publicationRepository);
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
    @DisplayName("validateReportIsReviewable() tests")
    class ValidateReportIsReviewableTests {

        @ParameterizedTest
        @EnumSource(value = ReportStatus.class, names = {"PENDING", "UNDER_REVIEW"})
        @DisplayName("Should pass when the report is not in a final status")
        void validateReportIsReviewable_WithOpenStatus_ShouldNotThrowException(ReportStatus status) {
            // Given
            testReport.setStatus(status);

            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateReportIsReviewable(testReport));
        }

        @ParameterizedTest
        @EnumSource(value = ReportStatus.class, names = {"RESOLVED", "REJECTED"})
        @DisplayName("Should throw ConflictException when the report is in a final status")
        void validateReportIsReviewable_WithFinalStatus_ShouldThrowConflictException(ReportStatus status) {
            // Given
            testReport.setStatus(status);

            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReportIsReviewable(testReport))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Report already reviewed with status: " + status.name());
        }
    }

    @Nested
    @DisplayName("validateReviewInput() tests")
    class ValidateReviewInputTests {

        @Test
        @DisplayName("Should throw MessageException when status is null")
        void validateReviewInput_WithNullStatus_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                    null, ActionTaken.USER_BANNED, ReportedEntityType.USER
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Status is required for review");
        }

        @Test
        @DisplayName("Should throw MessageException when status is PENDING")
        void validateReviewInput_WithPendingStatus_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                    ReportStatus.PENDING, ActionTaken.NO_ACTION, ReportedEntityType.USER
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cannot set status back to PENDING");
        }

        @ParameterizedTest
        @EnumSource(ReportedEntityType.class)
        @DisplayName("Should throw MessageException when RESOLVED with null action")
        void validateReviewInput_WithResolvedAndNullAction_ShouldThrowException(ReportedEntityType entityType) {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                    ReportStatus.RESOLVED, null, entityType
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Action taken must be specified when resolving a report");
        }

        @ParameterizedTest
        @MethodSource("com.vaPaTi.vaPaTi.validation.ReportValidationServiceTest#nonResolvedStatusesWithAction")
        @DisplayName("Should throw MessageException when UNDER_REVIEW or REJECTED carry an action")
        void validateReviewInput_WithNonResolvedStatusAndAction_ShouldThrowException(ReportStatus status, ActionTaken action) {
            // When & Then
            assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                    status, action, ReportedEntityType.USER
            ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Action taken can only be set when resolving a report");
        }

        @ParameterizedTest
        @MethodSource("com.vaPaTi.vaPaTi.validation.ReportValidationServiceTest#nonResolvedStatusesWithoutAction")
        @DisplayName("Should allow UNDER_REVIEW and REJECTED with a null or NO_ACTION action")
        void validateReviewInput_WithNonResolvedStatusAndNoAction_ShouldNotThrowException(ReportStatus status, ActionTaken action) {
            // When & Then
            assertDoesNotThrow(() -> reportValidationService.validateReviewInput(
                    status, action, ReportedEntityType.USER
            ));
        }

        @ParameterizedTest(name = "RESOLVED {0} + {1} -> {2}")
        @MethodSource("com.vaPaTi.vaPaTi.validation.ReportValidationServiceTest#resolvedActionMatrix")
        @DisplayName("Should apply the action matrix when RESOLVED (3 entity types x 6 actions)")
        void validateReviewInput_WithResolved_ShouldApplyActionMatrix(
                ReportedEntityType entityType, ActionTaken action, String expectedMessage) {
            if (expectedMessage == null) {
                assertDoesNotThrow(() -> reportValidationService.validateReviewInput(
                        ReportStatus.RESOLVED, action, entityType
                ));
            } else {
                assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                        ReportStatus.RESOLVED, action, entityType
                ))
                        .isInstanceOf(MessageException.class)
                        .hasMessage(expectedMessage);
            }
        }
    }

    static Stream<Arguments> nonResolvedStatusesWithAction() {
        return Stream.of(ReportStatus.UNDER_REVIEW, ReportStatus.REJECTED)
                .flatMap(status -> Arrays.stream(ActionTaken.values())
                        .filter(action -> action != ActionTaken.NO_ACTION)
                        .map(action -> Arguments.of(status, action)));
    }

    static Stream<Arguments> nonResolvedStatusesWithoutAction() {
        return Stream.of(
                Arguments.of(ReportStatus.UNDER_REVIEW, null),
                Arguments.of(ReportStatus.UNDER_REVIEW, ActionTaken.NO_ACTION),
                Arguments.of(ReportStatus.REJECTED, null),
                Arguments.of(ReportStatus.REJECTED, ActionTaken.NO_ACTION)
        );
    }

    static Stream<Arguments> resolvedActionMatrix() {
        String noAction = "Action taken must be specified when resolving a report";
        return Stream.of(
                Arguments.of(ReportedEntityType.USER, ActionTaken.NO_ACTION, noAction),
                Arguments.of(ReportedEntityType.USER, ActionTaken.WARNING_SENT, null),
                Arguments.of(ReportedEntityType.USER, ActionTaken.CONTENT_REMOVED, "Action CONTENT_REMOVED does not apply to a user"),
                Arguments.of(ReportedEntityType.USER, ActionTaken.USER_SUSPENDED, null),
                Arguments.of(ReportedEntityType.USER, ActionTaken.USER_BANNED, null),
                Arguments.of(ReportedEntityType.USER, ActionTaken.OTHER, null),
                Arguments.of(ReportedEntityType.PUBLICATION, ActionTaken.NO_ACTION, noAction),
                Arguments.of(ReportedEntityType.PUBLICATION, ActionTaken.WARNING_SENT, null),
                Arguments.of(ReportedEntityType.PUBLICATION, ActionTaken.CONTENT_REMOVED, null),
                Arguments.of(ReportedEntityType.PUBLICATION, ActionTaken.USER_SUSPENDED, "Action USER_SUSPENDED does not apply to a publication"),
                Arguments.of(ReportedEntityType.PUBLICATION, ActionTaken.USER_BANNED, "Action USER_BANNED does not apply to a publication"),
                Arguments.of(ReportedEntityType.PUBLICATION, ActionTaken.OTHER, null),
                Arguments.of(ReportedEntityType.CAMPAIGN, ActionTaken.NO_ACTION, noAction),
                Arguments.of(ReportedEntityType.CAMPAIGN, ActionTaken.WARNING_SENT, null),
                Arguments.of(ReportedEntityType.CAMPAIGN, ActionTaken.CONTENT_REMOVED, null),
                Arguments.of(ReportedEntityType.CAMPAIGN, ActionTaken.USER_SUSPENDED, "Action USER_SUSPENDED does not apply to a campaign"),
                Arguments.of(ReportedEntityType.CAMPAIGN, ActionTaken.USER_BANNED, "Action USER_BANNED does not apply to a campaign"),
                Arguments.of(ReportedEntityType.CAMPAIGN, ActionTaken.OTHER, null)
        );
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
