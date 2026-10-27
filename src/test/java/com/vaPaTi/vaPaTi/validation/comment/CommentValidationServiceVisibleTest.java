package com.vaPaTi.vaPaTi.validation.comment;

import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.comment.CommentRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentValidationService - Visible Comment Tests")
class CommentValidationServiceVisibleTest {

    private static final Long COMMENT_ID = 7L;
    private static final Long MISSING_COMMENT_ID = 999L;
    private static final Long PUBLICATION_ID = 10L;
    private static final Long AUTHOR_ID = 1L;
    private static final Long CALLER_ID = 2L;
    private static final String NOT_FOUND_MESSAGE = "Comment not found with ID: " + COMMENT_ID;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PublicationValidationService publicationValidationService;

    @Mock
    private CampaignAuthorizationService campaignAuthorizationService;

    @Mock
    private AccountStatusValidationService accountStatusValidationService;

    @Mock
    private TextValidationService textValidationService;

    @InjectMocks
    private CommentValidationService commentValidationService;

    private Comment comment;
    private User author;
    private Publication publication;

    @BeforeEach
    void setUp() {
        author = User.builder().id(AUTHOR_ID).build();
        publication = Publication.builder().id(PUBLICATION_ID).build();
        comment = Comment.builder()
                .id(COMMENT_ID)
                .text("Great work")
                .user(author)
                .publication(publication)
                .publicationId(PUBLICATION_ID)
                .build();
    }

    @Nested
    @DisplayName("validateAndGetVisibleComment")
    class ValidateAndGetVisibleComment {

        @Test
        @DisplayName("Should throw 404 when the comment does not exist, without looking at the publication")
        void shouldThrowNotFound_whenCommentDoesNotExist() {
            when(commentRepository.findById(MISSING_COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentValidationService.validateAndGetVisibleComment(MISSING_COMMENT_ID, CALLER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Comment not found with ID: " + MISSING_COMMENT_ID);
            verify(publicationValidationService, never()).findVisiblePublication(any(), any());
        }

        @Test
        @DisplayName("Should throw the same 404 when the author was deleted, without checking status or publication")
        void shouldThrowNotFound_whenAuthorIsDeleted() {
            comment.setUser(null);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentValidationService.validateAndGetVisibleComment(COMMENT_ID, CALLER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(NOT_FOUND_MESSAGE);
            verify(accountStatusValidationService, never()).isBlocked(any());
            verify(publicationValidationService, never()).findVisiblePublication(any(), any());
        }

        @Test
        @DisplayName("Should throw the same 404 when the author is blocked and the caller is not an ADMIN")
        void shouldThrowNotFound_whenAuthorIsBlockedAndCallerIsNotAdmin() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(accountStatusValidationService.isBlocked(author)).thenReturn(true);
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(false);

            assertThatThrownBy(() -> commentValidationService.validateAndGetVisibleComment(COMMENT_ID, CALLER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(NOT_FOUND_MESSAGE);
            verify(publicationValidationService, never()).findVisiblePublication(any(), any());
        }

        @Test
        @DisplayName("Should return the comment when the author is blocked and the caller is an ADMIN")
        void shouldReturnComment_whenAuthorIsBlockedAndCallerIsAdmin() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(accountStatusValidationService.isBlocked(author)).thenReturn(true);
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(true);
            when(publicationValidationService.findVisiblePublication(PUBLICATION_ID, CALLER_ID))
                    .thenReturn(Optional.of(publication));

            Comment result = commentValidationService.validateAndGetVisibleComment(COMMENT_ID, CALLER_ID);

            assertThat(result).isSameAs(comment);
        }

        @Test
        @DisplayName("Should not query the role when the author is active")
        void shouldNotQueryRole_whenAuthorIsActive() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(accountStatusValidationService.isBlocked(author)).thenReturn(false);
            when(publicationValidationService.findVisiblePublication(PUBLICATION_ID, CALLER_ID))
                    .thenReturn(Optional.of(publication));

            commentValidationService.validateAndGetVisibleComment(COMMENT_ID, CALLER_ID);

            verify(campaignAuthorizationService, never()).isAdmin(any());
        }

        @Test
        @DisplayName("Should throw the comment's 404, never the publication's, when the publication is not visible")
        void shouldThrowCommentNotFound_whenPublicationIsNotVisible() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(accountStatusValidationService.isBlocked(author)).thenReturn(false);
            when(publicationValidationService.findVisiblePublication(PUBLICATION_ID, CALLER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentValidationService.validateAndGetVisibleComment(COMMENT_ID, CALLER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(NOT_FOUND_MESSAGE)
                    .hasMessageNotContaining("Publication not found with ID: ");
        }

        @Test
        @DisplayName("Should pass the comment's publicationId and the callerId to findVisiblePublication")
        void shouldPassPublicationIdAndCallerId() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(accountStatusValidationService.isBlocked(author)).thenReturn(false);
            when(publicationValidationService.findVisiblePublication(any(), any())).thenReturn(Optional.of(publication));

            commentValidationService.validateAndGetVisibleComment(COMMENT_ID, CALLER_ID);

            ArgumentCaptor<Long> publicationIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> callerIdCaptor = ArgumentCaptor.forClass(Long.class);
            verify(publicationValidationService).findVisiblePublication(publicationIdCaptor.capture(), callerIdCaptor.capture());
            assertThat(publicationIdCaptor.getValue()).isEqualTo(PUBLICATION_ID);
            assertThat(callerIdCaptor.getValue()).isEqualTo(CALLER_ID);
        }

        @Test
        @DisplayName("Should return the comment when it is visible")
        void shouldReturnComment_whenVisible() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(accountStatusValidationService.isBlocked(author)).thenReturn(false);
            when(publicationValidationService.findVisiblePublication(PUBLICATION_ID, CALLER_ID))
                    .thenReturn(Optional.of(publication));

            Comment result = commentValidationService.validateAndGetVisibleComment(COMMENT_ID, CALLER_ID);

            assertThat(result).isSameAs(comment);
        }
    }

    @Nested
    @DisplayName("canSeeBlockedAuthors")
    class CanSeeBlockedAuthors {

        @Test
        @DisplayName("Should return false for an anonymous caller, without querying the role")
        void shouldReturnFalse_whenCallerIsAnonymous() {
            assertThat(commentValidationService.canSeeBlockedAuthors(null)).isFalse();
            verify(campaignAuthorizationService, never()).isAdmin(any());
        }

        @Test
        @DisplayName("Should return true for an ADMIN")
        void shouldReturnTrue_whenCallerIsAdmin() {
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(true);

            assertThat(commentValidationService.canSeeBlockedAuthors(CALLER_ID)).isTrue();
        }

        @Test
        @DisplayName("Should return false for a USER")
        void shouldReturnFalse_whenCallerIsUser() {
            when(campaignAuthorizationService.isAdmin(CALLER_ID)).thenReturn(false);

            assertThat(commentValidationService.canSeeBlockedAuthors(CALLER_ID)).isFalse();
        }
    }
}
