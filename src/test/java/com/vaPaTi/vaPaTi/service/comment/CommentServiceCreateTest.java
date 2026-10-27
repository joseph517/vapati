package com.vaPaTi.vaPaTi.service.comment;

import com.vaPaTi.vaPaTi.dtos.comment.CommentResponseDTO;
import com.vaPaTi.vaPaTi.dtos.comment.CreateCommentDTO;
import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.comment.CommentMapper;
import com.vaPaTi.vaPaTi.repository.comment.CommentRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.comment.CommentValidationService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Comment Service - Create Comment")
class CommentServiceCreateTest {

    private static final Long PUBLICATION_ID = 10L;
    private static final Long PUBLICATION_AUTHOR_ID = 1L;
    private static final Long CALLER_ID = 2L;
    private static final String RAW_TEXT = "  Great work  ";
    private static final String NORMALIZED_TEXT = "Great work";

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

    private Publication publication;
    private CreateCommentDTO dto;

    @BeforeEach
    void setUp() {
        User publicationAuthor = User.builder().id(PUBLICATION_AUTHOR_ID).build();
        publication = Publication.builder().id(PUBLICATION_ID).user(publicationAuthor).build();
        dto = new CreateCommentDTO();
        dto.setText(RAW_TEXT);
    }

    @Nested
    @DisplayName("createComment - happy path")
    class HappyPath {

        @Test
        @DisplayName("Should save the mapped entity built from the validated publication, the normalized text and the caller reference, and return its DTO")
        void shouldSaveMappedEntityAndReturnDto() {
            User callerReference = User.builder().id(CALLER_ID).build();
            Comment mapped = Comment.builder().text(NORMALIZED_TEXT).build();
            Comment saved = Comment.builder().id(7L).text(NORMALIZED_TEXT).build();
            CommentResponseDTO response = new CommentResponseDTO();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID))
                    .thenReturn(publication);
            when(commentValidationService.validateAndNormalizeText(RAW_TEXT)).thenReturn(NORMALIZED_TEXT);
            when(userRepository.getReferenceById(CALLER_ID)).thenReturn(callerReference);
            when(commentMapper.toEntity(NORMALIZED_TEXT, publication, callerReference)).thenReturn(mapped);
            when(commentRepository.save(mapped)).thenReturn(saved);
            when(commentMapper.toDTO(saved)).thenReturn(response);

            CommentResponseDTO result = commentService.createComment(PUBLICATION_ID, dto);

            assertThat(result).isSameAs(response);
            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(mapped);
            verify(commentMapper).toDTO(saved);
        }

        @Test
        @DisplayName("Should take the callerId from the token and pass it to validateAndGetVisiblePublication")
        void shouldPassAuthenticatedCallerIdToVisibilityCheck() {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(publicationValidationService.validateAndGetVisiblePublication(any(), any())).thenReturn(publication);
            when(commentValidationService.validateAndNormalizeText(RAW_TEXT)).thenReturn(NORMALIZED_TEXT);

            commentService.createComment(PUBLICATION_ID, dto);

            ArgumentCaptor<Long> publicationIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> callerIdCaptor = ArgumentCaptor.forClass(Long.class);
            verify(publicationValidationService)
                    .validateAndGetVisiblePublication(publicationIdCaptor.capture(), callerIdCaptor.capture());
            assertThat(publicationIdCaptor.getValue()).isEqualTo(PUBLICATION_ID);
            assertThat(callerIdCaptor.getValue()).isEqualTo(CALLER_ID);
            verify(userRepository).getReferenceById(CALLER_ID);
        }

        @Test
        @DisplayName("Should save the comment when the caller is the publication author")
        void shouldSave_whenCallerIsPublicationAuthor() {
            User authorReference = User.builder().id(PUBLICATION_AUTHOR_ID).build();
            Comment mapped = Comment.builder().text(NORMALIZED_TEXT).build();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(PUBLICATION_AUTHOR_ID);
            when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, PUBLICATION_AUTHOR_ID))
                    .thenReturn(publication);
            when(commentValidationService.validateAndNormalizeText(RAW_TEXT)).thenReturn(NORMALIZED_TEXT);
            when(userRepository.getReferenceById(PUBLICATION_AUTHOR_ID)).thenReturn(authorReference);
            when(commentMapper.toEntity(NORMALIZED_TEXT, publication, authorReference)).thenReturn(mapped);

            commentService.createComment(PUBLICATION_ID, dto);

            verify(commentRepository).save(mapped);
        }
    }

    @Nested
    @DisplayName("createComment - errors")
    class Errors {

        @Test
        @DisplayName("Should throw the publication's 404 when it is hidden, without validating the text nor saving")
        void shouldThrowPublicationNotFound_whenPublicationIsHidden() {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID))
                    .thenThrow(new ResourceNotFoundException("Publication not found with ID: " + PUBLICATION_ID));
            dto.setText("   ");

            assertThatThrownBy(() -> commentService.createComment(PUBLICATION_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Publication not found with ID: " + PUBLICATION_ID);
            verify(commentValidationService, never()).validateAndNormalizeText(any());
            verify(userRepository, never()).getReferenceById(any());
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw 400 when the text is blank, without saving nor loading the caller reference")
        void shouldThrowBadRequest_whenTextIsBlank() {
            dto.setText("   ");
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID))
                    .thenReturn(publication);
            when(commentValidationService.validateAndNormalizeText("   "))
                    .thenThrow(new MessageException("Text cannot be blank"));

            assertThatThrownBy(() -> commentService.createComment(PUBLICATION_ID, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Text cannot be blank");
            verify(userRepository, never()).getReferenceById(any());
            verify(commentMapper, never()).toEntity(any(), any(), any());
            verify(commentRepository, never()).save(any());
        }
    }
}
