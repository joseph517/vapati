package com.vaPaTi.vaPaTi.service.publication;

import com.vaPaTi.vaPaTi.dtos.publication.PublicationLikeResponseDTO;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publication Like Service - Unlike Publication")
class PublicationLikeServiceUnlikeTest {

    private static final Long PUBLICATION_ID = 10L;
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

    @BeforeEach
    void setUp() {
        publication = Publication.builder()
                .id(PUBLICATION_ID)
                .description("Test publication")
                .user(User.builder().id(1L).build())
                .campaignId(5L)
                .build();
    }

    @Test
    @DisplayName("Should delete the caller's like and answer the visible count with likedByMe false")
    void unlikePublication_WithLike_ShouldDeleteLike() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID)).thenReturn(publication);
        when(publicationLikeRepository.deleteByPublicationIdAndUserId(PUBLICATION_ID, CALLER_ID)).thenReturn(1);
        when(publicationLikeRepository.countVisibleByPublicationId(eq(PUBLICATION_ID), any(LocalDateTime.class))).thenReturn(2L);

        // When
        PublicationLikeResponseDTO result = publicationLikeService.unlikePublication(PUBLICATION_ID);

        // Then
        verify(publicationLikeRepository).deleteByPublicationIdAndUserId(PUBLICATION_ID, CALLER_ID);
        assertThat(result.getPublicationId()).isEqualTo(PUBLICATION_ID);
        assertThat(result.getLikesCount()).isEqualTo(2L);
        assertThat(result.getLikedByMe()).isFalse();
    }

    @Test
    @DisplayName("Should answer the same way when there was no like (0 rows deleted)")
    void unlikePublication_WithoutLike_ShouldAnswerLikedByMeFalse() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID)).thenReturn(publication);
        when(publicationLikeRepository.deleteByPublicationIdAndUserId(PUBLICATION_ID, CALLER_ID)).thenReturn(0);
        when(publicationLikeRepository.countVisibleByPublicationId(eq(PUBLICATION_ID), any(LocalDateTime.class))).thenReturn(0L);

        // When
        PublicationLikeResponseDTO result = publicationLikeService.unlikePublication(PUBLICATION_ID);

        // Then
        verify(publicationLikeRepository).deleteByPublicationIdAndUserId(PUBLICATION_ID, CALLER_ID);
        assertThat(result.getPublicationId()).isEqualTo(PUBLICATION_ID);
        assertThat(result.getLikesCount()).isZero();
        assertThat(result.getLikedByMe()).isFalse();
    }

    @Test
    @DisplayName("Should neither delete nor count likes when the publication is not visible")
    void unlikePublication_WhenPublicationNotVisible_ShouldThrowNotFound() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID))
                .thenThrow(new ResourceNotFoundException("Publication not found with ID: " + PUBLICATION_ID));

        // When / Then
        assertThatThrownBy(() -> publicationLikeService.unlikePublication(PUBLICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Publication not found with ID: 10");
        verifyNoInteractions(publicationLikeRepository, userRepository);
    }
}
