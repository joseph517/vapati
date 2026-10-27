package com.vaPaTi.vaPaTi.validation.report;

import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.entity.report.ActionTaken;
import com.vaPaTi.vaPaTi.entity.report.ReportStatus;
import com.vaPaTi.vaPaTi.entity.report.ReportedEntityType;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.publication.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.report.ReportRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import com.vaPaTi.vaPaTi.validation.comment.CommentValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportValidationService - Comment Tests")
class ReportValidationServiceCommentTest {

    private static final Long COMMENT_ID = 7L;
    private static final Long REPORTER_ID = 1L;
    private static final Long AUTHOR_ID = 2L;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PublicationRepository publicationRepository;
    @Mock
    private CampaignValidationService campaignValidationService;
    @Mock
    private AccountStatusValidationService accountStatusValidationService;
    @Mock
    private CommentValidationService commentValidationService;

    @InjectMocks
    private ReportValidationService reportValidationService;

    private Comment commentBy(Long authorId) {
        User author = new User();
        author.setId(authorId);
        Comment comment = new Comment();
        comment.setId(COMMENT_ID);
        comment.setUser(author);
        return comment;
    }

    @Nested
    @DisplayName("validateEntityExists(COMMENT)")
    class ValidateEntityExists {

        @Test
        @DisplayName("Should delegate to the comment validation and return the author id")
        void validateEntityExists_WithVisibleComment_ShouldReturnAuthorId() {
            when(commentValidationService.validateAndGetVisibleComment(COMMENT_ID, REPORTER_ID))
                    .thenReturn(commentBy(AUTHOR_ID));

            Long ownerId = reportValidationService.validateEntityExists(ReportedEntityType.COMMENT, COMMENT_ID, REPORTER_ID);

            assertThat(ownerId).isEqualTo(AUTHOR_ID);
            verify(commentValidationService).validateAndGetVisibleComment(COMMENT_ID, REPORTER_ID);
            verifyNoInteractions(publicationRepository, userRepository, campaignValidationService);
        }

        @Test
        @DisplayName("Should propagate the 404 of a missing or hidden comment with its literal")
        void validateEntityExists_WithHiddenComment_ShouldThrowNotFound() {
            when(commentValidationService.validateAndGetVisibleComment(COMMENT_ID, REPORTER_ID))
                    .thenThrow(new ResourceNotFoundException("Comment not found with ID: " + COMMENT_ID));

            assertThatThrownBy(() -> reportValidationService.validateEntityExists(
                    ReportedEntityType.COMMENT, COMMENT_ID, REPORTER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Comment not found with ID: 7");
        }
    }

    @Nested
    @DisplayName("validateNotSelfReport(COMMENT)")
    class ValidateNotSelfReport {

        @Test
        @DisplayName("Should throw when the reporter is the author of the comment")
        void validateNotSelfReport_WithOwnComment_ShouldThrowException() {
            assertThatThrownBy(() -> reportValidationService.validateNotSelfReport(
                    REPORTER_ID, ReportedEntityType.COMMENT, REPORTER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("You cannot report your own comment");
        }

        @Test
        @DisplayName("Should pass when the comment belongs to someone else")
        void validateNotSelfReport_WithOtherAuthor_ShouldNotThrowException() {
            assertDoesNotThrow(() -> reportValidationService.validateNotSelfReport(
                    REPORTER_ID, ReportedEntityType.COMMENT, AUTHOR_ID));
        }

        @Test
        @DisplayName("Should pass when the owner is null")
        void validateNotSelfReport_WithNullOwner_ShouldNotThrowException() {
            assertDoesNotThrow(() -> reportValidationService.validateNotSelfReport(
                    REPORTER_ID, ReportedEntityType.COMMENT, null));
        }
    }

    @Nested
    @DisplayName("validateReviewInput(RESOLVED, *, COMMENT)")
    class ValidateReviewInput {

        @ParameterizedTest(name = "RESOLVED + {0} is valid")
        @EnumSource(value = ActionTaken.class, names = {"CONTENT_REMOVED", "WARNING_SENT", "OTHER"})
        @DisplayName("Should accept the content actions")
        void validateReviewInput_WithContentAction_ShouldNotThrowException(ActionTaken action) {
            assertDoesNotThrow(() -> reportValidationService.validateReviewInput(
                    ReportStatus.RESOLVED, action, ReportedEntityType.COMMENT));
        }

        @ParameterizedTest(name = "RESOLVED + {0} is rejected")
        @EnumSource(value = ActionTaken.class, names = {"USER_BANNED", "USER_SUSPENDED"})
        @DisplayName("Should reject the user actions")
        void validateReviewInput_WithUserAction_ShouldThrowException(ActionTaken action) {
            assertThatThrownBy(() -> reportValidationService.validateReviewInput(
                    ReportStatus.RESOLVED, action, ReportedEntityType.COMMENT))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Action " + action.name() + " does not apply to a comment");
        }
    }

    @Nested
    @DisplayName("validateNoDuplicateReport(COMMENT)")
    class ValidateNoDuplicateReport {

        @Test
        @DisplayName("Should throw ConflictException when the comment was already reported")
        void validateNoDuplicateReport_WithDuplicate_ShouldThrowConflict() {
            when(reportRepository.existsByReporterIdAndReportedEntityTypeAndReportedEntityId(
                    REPORTER_ID, ReportedEntityType.COMMENT, COMMENT_ID)).thenReturn(true);

            assertThatThrownBy(() -> reportValidationService.validateNoDuplicateReport(
                    REPORTER_ID, ReportedEntityType.COMMENT, COMMENT_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("You have already reported this comment");
        }
    }
}
