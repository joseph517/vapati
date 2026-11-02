package com.vaPaTi.vaPaTi.service.report;

import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.entity.report.ActionTaken;
import com.vaPaTi.vaPaTi.entity.report.Report;
import com.vaPaTi.vaPaTi.entity.report.ReportedEntityType;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.repository.campaign.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.comment.CommentRepository;
import com.vaPaTi.vaPaTi.repository.publication.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.service.campaign.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportActionService - Comment Tests")
class ReportActionServiceCommentTest {

    private static final Long COMMENT_ID = 7L;
    private static final Long ADMIN_ID = 99L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PublicationRepository publicationRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CampaignService campaignService;
    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ReportActionService reportActionService;

    private Report report;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setId(ADMIN_ID);

        report = new Report();
        report.setId(1L);
        report.setReportedEntityType(ReportedEntityType.COMMENT);
        report.setReportedEntityId(COMMENT_ID);
        report.setReviewedBy(admin);
    }

    @Nested
    @DisplayName("CONTENT_REMOVED on a comment")
    class ContentRemoved {

        @Test
        @DisplayName("Should delete the comment found by id")
        void executeAction_WithContentRemovedOnComment_ShouldDeleteComment() {
            Comment comment = new Comment();
            comment.setId(COMMENT_ID);
            report.setActionTaken(ActionTaken.CONTENT_REMOVED);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

            reportActionService.executeAction(report);

            verify(commentRepository).delete(comment);
            verifyNoInteractions(publicationRepository, campaignRepository, campaignService, userRepository);
        }

        @Test
        @DisplayName("Should do nothing when the comment was already removed")
        void executeAction_WithContentRemovedOnMissingComment_ShouldNotDelete() {
            report.setActionTaken(ActionTaken.CONTENT_REMOVED);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            reportActionService.executeAction(report);

            verify(commentRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("User sanctions on a comment")
    class UserSanctions {

        @ParameterizedTest(name = "{0} on a comment does nothing")
        @EnumSource(value = ActionTaken.class, names = {"USER_BANNED", "USER_SUSPENDED"})
        @DisplayName("Should not touch users nor comments")
        void executeAction_WithUserSanctionOnComment_ShouldDoNothing(ActionTaken action) {
            report.setActionTaken(action);

            reportActionService.executeAction(report);

            verifyNoInteractions(userRepository, commentRepository);
        }
    }
}
