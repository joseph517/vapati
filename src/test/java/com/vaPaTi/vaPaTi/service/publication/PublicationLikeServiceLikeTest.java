package com.vaPaTi.vaPaTi.service.publication;

import com.vaPaTi.vaPaTi.dtos.publication.PublicationLikeResponseDTO;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.publication.PublicationLike;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.publication.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.publication.PublicationLikeRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publication Like Service - Like Publication")
class PublicationLikeServiceLikeTest {

    private static final Long PUBLICATION_ID = 10L;
    private static final Long AUTHOR_ID = 1L;
    private static final Long CALLER_ID = 2L;

    @Mock
    private PublicationLikeRepository publicationLikeRepository;

    @Mock
    private PublicationValidationService publicationValidationService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private UserRepository userRepository;

    @Spy
    private PublicationMapper publicationMapper = new PublicationMapper();

    @InjectMocks
    private PublicationLikeService publicationLikeService;

    private Publication publication;
    private User callerReference;

    @BeforeEach
    void setUp() {
        User author = User.builder().id(AUTHOR_ID).build();
        callerReference = User.builder().id(CALLER_ID).build();
        publication = Publication.builder()
                .id(PUBLICATION_ID)
                .description("Test publication")
                .user(author)
                .campaignId(5L)
                .build();
    }

    @Test
    @DisplayName("Should save the like and answer the visible count with likedByMe true when there was no like")
    void likePublication_WithoutPreviousLike_ShouldSaveLike() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID)).thenReturn(publication);
        when(publicationLikeRepository.existsByPublication_IdAndUser_Id(PUBLICATION_ID, CALLER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(CALLER_ID)).thenReturn(callerReference);
        when(publicationLikeRepository.countVisibleByPublicationId(eq(PUBLICATION_ID), any(LocalDateTime.class))).thenReturn(3L);

        // When
        PublicationLikeResponseDTO result = publicationLikeService.likePublication(PUBLICATION_ID);

        // Then
        ArgumentCaptor<PublicationLike> captor = ArgumentCaptor.forClass(PublicationLike.class);
        verify(publicationLikeRepository).save(captor.capture());
        assertThat(captor.getValue().getPublication()).isSameAs(publication);
        assertThat(captor.getValue().getUser()).isSameAs(callerReference);

        assertThat(result.getPublicationId()).isEqualTo(PUBLICATION_ID);
        assertThat(result.getLikesCount()).isEqualTo(3L);
        assertThat(result.getLikedByMe()).isTrue();
    }

    @Test
    @DisplayName("Should not save nor load the user reference when the like already exists")
    void likePublication_WithPreviousLike_ShouldNotSave() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID)).thenReturn(publication);
        when(publicationLikeRepository.existsByPublication_IdAndUser_Id(PUBLICATION_ID, CALLER_ID)).thenReturn(true);
        when(publicationLikeRepository.countVisibleByPublicationId(eq(PUBLICATION_ID), any(LocalDateTime.class))).thenReturn(1L);

        // When
        PublicationLikeResponseDTO result = publicationLikeService.likePublication(PUBLICATION_ID);

        // Then
        verify(publicationLikeRepository, never()).save(any());
        verify(userRepository, never()).getReferenceById(any());
        assertThat(result.getLikesCount()).isEqualTo(1L);
        assertThat(result.getLikedByMe()).isTrue();
    }

    @Test
    @DisplayName("Should not rethrow and answer the re-read count when a concurrent request saved the like first")
    void likePublication_WhenSaveHitsUniqueConstraint_ShouldAnswerReReadState() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID)).thenReturn(publication);
        when(publicationLikeRepository.existsByPublication_IdAndUser_Id(PUBLICATION_ID, CALLER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(CALLER_ID)).thenReturn(callerReference);
        when(publicationLikeRepository.save(any(PublicationLike.class)))
                .thenThrow(new DataIntegrityViolationException("uk_publication_like_publication_user"));
        when(publicationLikeRepository.countVisibleByPublicationId(eq(PUBLICATION_ID), any(LocalDateTime.class))).thenReturn(4L);

        // When
        PublicationLikeResponseDTO result = publicationLikeService.likePublication(PUBLICATION_ID);

        // Then
        verify(publicationLikeRepository).countVisibleByPublicationId(eq(PUBLICATION_ID), any(LocalDateTime.class));
        assertThat(result.getPublicationId()).isEqualTo(PUBLICATION_ID);
        assertThat(result.getLikesCount()).isEqualTo(4L);
        assertThat(result.getLikedByMe()).isTrue();
    }

    @Test
    @DisplayName("Should neither read nor write likes when the publication is not visible")
    void likePublication_WhenPublicationNotVisible_ShouldThrowNotFound() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID))
                .thenThrow(new ResourceNotFoundException("Publication not found with ID: " + PUBLICATION_ID));

        // When / Then
        assertThatThrownBy(() -> publicationLikeService.likePublication(PUBLICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Publication not found with ID: 10");
        verifyNoInteractions(publicationLikeRepository, userRepository);
        verify(publicationMapper, never()).toLikeResponseDTO(any(), any(Long.class), any(Boolean.class));
    }

    @Test
    @DisplayName("Should save the like when the author likes their own publication")
    void likePublication_WhenCallerIsAuthor_ShouldSaveLike() {
        // Given
        User authorReference = User.builder().id(AUTHOR_ID).build();
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(AUTHOR_ID);
        when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, AUTHOR_ID)).thenReturn(publication);
        when(publicationLikeRepository.existsByPublication_IdAndUser_Id(PUBLICATION_ID, AUTHOR_ID)).thenReturn(false);
        when(userRepository.getReferenceById(AUTHOR_ID)).thenReturn(authorReference);
        when(publicationLikeRepository.countVisibleByPublicationId(eq(PUBLICATION_ID), any(LocalDateTime.class))).thenReturn(1L);

        // When
        PublicationLikeResponseDTO result = publicationLikeService.likePublication(PUBLICATION_ID);

        // Then
        ArgumentCaptor<PublicationLike> captor = ArgumentCaptor.forClass(PublicationLike.class);
        verify(publicationLikeRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(authorReference);
        assertThat(result.getLikedByMe()).isTrue();
    }

    @Test
    @DisplayName("Should pass the caller id from the token to validateAndGetVisiblePublication")
    void likePublication_ShouldUseCallerIdFromToken() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(publicationValidationService.validateAndGetVisiblePublication(any(), any())).thenReturn(publication);
        when(publicationLikeRepository.existsByPublication_IdAndUser_Id(PUBLICATION_ID, CALLER_ID)).thenReturn(true);
        when(publicationLikeRepository.countVisibleByPublicationId(eq(PUBLICATION_ID), any(LocalDateTime.class))).thenReturn(1L);

        // When
        publicationLikeService.likePublication(PUBLICATION_ID);

        // Then
        ArgumentCaptor<Long> publicationIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> callerIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(publicationValidationService).validateAndGetVisiblePublication(publicationIdCaptor.capture(), callerIdCaptor.capture());
        assertThat(publicationIdCaptor.getValue()).isEqualTo(PUBLICATION_ID);
        assertThat(callerIdCaptor.getValue()).isEqualTo(CALLER_ID);
    }
}
