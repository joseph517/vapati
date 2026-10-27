package com.vaPaTi.vaPaTi.service.comment;

import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.comment.CommentMapper;
import com.vaPaTi.vaPaTi.repository.comment.CommentRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.comment.CommentValidationService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Comment Service - Delete Comment")
class CommentServiceDeleteTest {

    private static final Long COMMENT_ID = 7L;
    private static final Long CALLER_ID = 2L;
    private static final String FORBIDDEN_MESSAGE =
            "Only the comment author, the publication author or an admin can delete this comment";

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentValidationService commentValidationService;

    @Mock
    private PublicationValidationService publicationValidationService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    @Nested
    @DisplayName("deleteComment - happy path")
    class HappyPath {

        @Test
        @DisplayName("Should delete the comment returned by validateAndGetDeletableComment for the authenticated caller")
        void shouldDeleteValidatedComment() {
            Comment comment = Comment.builder().id(COMMENT_ID).build();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(commentValidationService.validateAndGetDeletableComment(COMMENT_ID, CALLER_ID)).thenReturn(comment);

            commentService.deleteComment(COMMENT_ID);

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).delete(captor.capture());
            assertThat(captor.getValue()).isSameAs(comment);
        }
    }

    @Nested
    @DisplayName("deleteComment - errors")
    class Errors {

        @Test
        @DisplayName("Should not delete when the validation throws 403")
        void shouldNotDelete_whenForbidden() {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(commentValidationService.validateAndGetDeletableComment(COMMENT_ID, CALLER_ID))
                    .thenThrow(new ForbiddenActionException(FORBIDDEN_MESSAGE));

            assertThatThrownBy(() -> commentService.deleteComment(COMMENT_ID))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage(FORBIDDEN_MESSAGE);
            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should not delete when the validation throws the comment's 404")
        void shouldNotDelete_whenNotFound() {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(commentValidationService.validateAndGetDeletableComment(COMMENT_ID, CALLER_ID))
                    .thenThrow(new ResourceNotFoundException("Comment not found with ID: " + COMMENT_ID));

            assertThatThrownBy(() -> commentService.deleteComment(COMMENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Comment not found with ID: " + COMMENT_ID);
            verify(commentRepository, never()).delete(any());
        }
    }
}
