package com.vaPaTi.vaPaTi.service.comment;

import com.vaPaTi.vaPaTi.dtos.comment.CommentResponseDTO;
import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Comment Service - List Comments")
class CommentServiceListTest {

    private static final Long PUBLICATION_ID = 10L;
    private static final Long USER_ID = 2L;
    private static final Long ADMIN_ID = 3L;

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

    private Pageable pageable;
    private Publication publication;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);
        publication = Publication.builder().id(PUBLICATION_ID).build();
    }

    @Nested
    @DisplayName("getCommentsByPublicationId - query selection")
    class QuerySelection {

        @Test
        @DisplayName("Should use the visible query with the received Pageable and a non-null now for an anonymous caller")
        void shouldUseVisibleQuery_whenCallerIsAnonymous() {
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
            when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, null))
                    .thenReturn(publication);
            when(commentValidationService.canSeeSanctionedAuthors(null)).thenReturn(false);
            when(commentRepository.findVisibleByPublicationId(eq(PUBLICATION_ID), any(), any()))
                    .thenReturn(Page.empty(pageable));

            commentService.getCommentsByPublicationId(PUBLICATION_ID, pageable);

            ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(commentRepository).findVisibleByPublicationId(eq(PUBLICATION_ID), nowCaptor.capture(),
                    pageableCaptor.capture());
            assertThat(nowCaptor.getValue()).isNotNull();
            assertThat(pageableCaptor.getValue()).isSameAs(pageable);
            verify(commentRepository, never()).findAllByPublicationId(any(), any());
        }

        @Test
        @DisplayName("Should use the visible query for a USER")
        void shouldUseVisibleQuery_whenCallerIsUser() {
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(USER_ID));
            when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, USER_ID))
                    .thenReturn(publication);
            when(commentValidationService.canSeeSanctionedAuthors(USER_ID)).thenReturn(false);
            when(commentRepository.findVisibleByPublicationId(eq(PUBLICATION_ID), any(), eq(pageable)))
                    .thenReturn(Page.empty(pageable));

            commentService.getCommentsByPublicationId(PUBLICATION_ID, pageable);

            verify(commentRepository).findVisibleByPublicationId(eq(PUBLICATION_ID), any(), eq(pageable));
            verify(commentRepository, never()).findAllByPublicationId(any(), any());
        }

        @Test
        @DisplayName("Should use the query without author status for an ADMIN, never the visible one")
        void shouldUseAllQuery_whenCallerIsAdmin() {
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(ADMIN_ID));
            when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, ADMIN_ID))
                    .thenReturn(publication);
            when(commentValidationService.canSeeSanctionedAuthors(ADMIN_ID)).thenReturn(true);
            when(commentRepository.findAllByPublicationId(PUBLICATION_ID, pageable)).thenReturn(Page.empty(pageable));

            commentService.getCommentsByPublicationId(PUBLICATION_ID, pageable);

            verify(commentRepository).findAllByPublicationId(PUBLICATION_ID, pageable);
            verify(commentRepository, never()).findVisibleByPublicationId(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getCommentsByPublicationId - result")
    class Result {

        @Test
        @DisplayName("Should map every element with toDTO and keep totalElements")
        void shouldMapEveryElementAndKeepTotal() {
            Comment first = Comment.builder().id(1L).build();
            Comment second = Comment.builder().id(2L).build();
            CommentResponseDTO firstDto = new CommentResponseDTO();
            CommentResponseDTO secondDto = new CommentResponseDTO();
            Page<Comment> page = new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 5);
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(USER_ID));
            when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, USER_ID))
                    .thenReturn(publication);
            when(commentValidationService.canSeeSanctionedAuthors(USER_ID)).thenReturn(false);
            when(commentRepository.findVisibleByPublicationId(eq(PUBLICATION_ID), any(), any())).thenReturn(page);
            when(commentMapper.toDTO(first)).thenReturn(firstDto);
            when(commentMapper.toDTO(second)).thenReturn(secondDto);

            Page<CommentResponseDTO> result =
                    commentService.getCommentsByPublicationId(PUBLICATION_ID, PageRequest.of(0, 2));

            assertThat(result.getContent()).containsExactly(firstDto, secondDto);
            assertThat(result.getTotalElements()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw the publication's 404 when it is hidden, without touching the comments")
        void shouldThrowNotFound_whenPublicationIsHidden() {
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
            when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, null))
                    .thenThrow(new ResourceNotFoundException("Publication not found with ID: " + PUBLICATION_ID));

            assertThatThrownBy(() -> commentService.getCommentsByPublicationId(PUBLICATION_ID, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Publication not found with ID: " + PUBLICATION_ID);
            verifyNoInteractions(commentRepository);
            verifyNoInteractions(commentMapper);
        }
    }
}
