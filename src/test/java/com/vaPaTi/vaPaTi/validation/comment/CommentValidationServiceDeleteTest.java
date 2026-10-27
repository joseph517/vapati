package com.vaPaTi.vaPaTi.validation.comment;

import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
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
@DisplayName("CommentValidationService - Deletable Comment Tests")
class CommentValidationServiceDeleteTest {

    private static final Long COMMENT_ID = 7L;
    private static final Long MISSING_COMMENT_ID = 999L;
    private static final Long PUBLICATION_ID = 10L;
    private static final Long COMMENT_AUTHOR_ID = 1L;
    private static final Long PUBLICATION_AUTHOR_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;
    private static final String NOT_FOUND_MESSAGE = "Comment not found with ID: " + COMMENT_ID;
    private static final String FORBIDDEN_MESSAGE =
            "Only the comment author, the publication author or an admin can delete this comment";

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
    private User commentAuthor;
    private Publication publication;

    @BeforeEach
    void setUp() {
        commentAuthor = User.builder().id(COMMENT_AUTHOR_ID).build();
        User publicationAuthor = User.builder().id(PUBLICATION_AUTHOR_ID).build();
        publication = Publication.builder().id(PUBLICATION_ID).user(publicationAuthor).build();
        comment = Comment.builder()
                .id(COMMENT_ID)
                .text("Great work")
                .user(commentAuthor)
                .publication(publication)
                .publicationId(PUBLICATION_ID)
                .build();
    }

    private void givenVisibleComment(Long callerId) {
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(accountStatusValidationService.isBlocked(commentAuthor)).thenReturn(false);
        when(publicationValidationService.findVisiblePublication(PUBLICATION_ID, callerId))
                .thenReturn(Optional.of(publication));
    }

    @Nested
    @DisplayName("validateAndGetDeletableComment - allowed callers")
    class AllowedCallers {

        @Test
        @DisplayName("Should return the comment for its author, without querying the role")
        void shouldReturnComment_whenCallerIsCommentAuthor() {
            givenVisibleComment(COMMENT_AUTHOR_ID);

            Comment result = commentValidationService.validateAndGetDeletableComment(COMMENT_ID, COMMENT_AUTHOR_ID);

            assertThat(result).isSameAs(comment);
            verify(campaignAuthorizationService, never()).isAdmin(any());
        }

        @Test
        @DisplayName("Should return the comment for the publication author, without querying the role")
        void shouldReturnComment_whenCallerIsPublicationAuthor() {
            givenVisibleComment(PUBLICATION_AUTHOR_ID);

            Comment result = commentValidationService.validateAndGetDeletableComment(COMMENT_ID, PUBLICATION_AUTHOR_ID);

            assertThat(result).isSameAs(comment);
            verify(campaignAuthorizationService, never()).isAdmin(any());
        }

        @Test
        @DisplayName("Should return the comment for an ADMIN who is neither author")
        void shouldReturnComment_whenCallerIsAdmin() {
            givenVisibleComment(OTHER_USER_ID);
            when(campaignAuthorizationService.isAdmin(OTHER_USER_ID)).thenReturn(true);

            Comment result = commentValidationService.validateAndGetDeletableComment(COMMENT_ID, OTHER_USER_ID);

            assertThat(result).isSameAs(comment);
        }

        @Test
        @DisplayName("Should return the comment of a blocked author for an ADMIN")
        void shouldReturnComment_whenAuthorIsBlockedAndCallerIsAdmin() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(accountStatusValidationService.isBlocked(commentAuthor)).thenReturn(true);
            when(campaignAuthorizationService.isAdmin(OTHER_USER_ID)).thenReturn(true);
            when(publicationValidationService.findVisiblePublication(PUBLICATION_ID, OTHER_USER_ID))
                    .thenReturn(Optional.of(publication));

            Comment result = commentValidationService.validateAndGetDeletableComment(COMMENT_ID, OTHER_USER_ID);

            assertThat(result).isSameAs(comment);
        }
    }

    @Nested
    @DisplayName("validateAndGetDeletableComment - rejected callers")
    class RejectedCallers {

        @Test
        @DisplayName("Should throw 403 when the caller is not an author nor an ADMIN")
        void shouldThrowForbidden_whenCallerIsThirdParty() {
            givenVisibleComment(OTHER_USER_ID);
            when(campaignAuthorizationService.isAdmin(OTHER_USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> commentValidationService.validateAndGetDeletableComment(COMMENT_ID, OTHER_USER_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage(FORBIDDEN_MESSAGE);
        }

        @Test
        @DisplayName("Should throw the comment's 404, not the 403, when the comment does not exist")
        void shouldThrowNotFound_whenCommentDoesNotExist() {
            when(commentRepository.findById(MISSING_COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentValidationService.validateAndGetDeletableComment(MISSING_COMMENT_ID, OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Comment not found with ID: " + MISSING_COMMENT_ID);
            verify(campaignAuthorizationService, never()).isAdmin(any());
        }

        @Test
        @DisplayName("Should throw the comment's 404, not the 403, when the publication is not visible")
        void shouldThrowNotFound_whenPublicationIsNotVisible() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(accountStatusValidationService.isBlocked(commentAuthor)).thenReturn(false);
            when(publicationValidationService.findVisiblePublication(PUBLICATION_ID, OTHER_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentValidationService.validateAndGetDeletableComment(COMMENT_ID, OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(NOT_FOUND_MESSAGE);
            verify(campaignAuthorizationService, never()).isAdmin(any());
        }

        @Test
        @DisplayName("Should throw the comment's 404 even for the comment author when the publication is not visible")
        void shouldThrowNotFound_whenCommentAuthorCannotSeePublication() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(accountStatusValidationService.isBlocked(commentAuthor)).thenReturn(false);
            when(publicationValidationService.findVisiblePublication(PUBLICATION_ID, COMMENT_AUTHOR_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentValidationService.validateAndGetDeletableComment(COMMENT_ID, COMMENT_AUTHOR_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(NOT_FOUND_MESSAGE);
        }
    }
}
