package com.vaPaTi.vaPaTi.service.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaPaTi.vaPaTi.dtos.publication.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.publication.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.publication.PublicationLikeRepository;
import com.vaPaTi.vaPaTi.repository.publication.PublicationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publication Service - Get Publications By Campaign")
class PublicationServiceGetByCampaignTest {

    private static final Long CAMPAIGN_ID = 5L;
    private static final Long CALLER_ID = 2L;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private PublicationValidationService publicationValidationService;

    @Mock
    private PublicationLikeRepository publicationLikeRepository;

    @Mock
    private PublicationMapper publicationMapper;

    private PublicationService publicationService;

    private Campaign campaign;
    private Publication newer;
    private Publication older;
    private PublicationResponseDTO newerDto;
    private PublicationResponseDTO olderDto;

    @BeforeEach
    void setUp() {
        publicationService = new PublicationService(publicationRepository, publicationMapper,
                authenticatedUserService, publicationValidationService, publicationLikeRepository);

        campaign = Campaign.builder().id(CAMPAIGN_ID).build();
        newer = Publication.builder().id(11L).campaignId(CAMPAIGN_ID).build();
        older = Publication.builder().id(10L).campaignId(CAMPAIGN_ID).build();
        newerDto = PublicationResponseDTO.builder().id(11L).campaignId(CAMPAIGN_ID).build();
        olderDto = PublicationResponseDTO.builder().id(10L).campaignId(CAMPAIGN_ID).build();
    }

    @Test
    @DisplayName("Should validate the campaign with the token user and map the publications in the repository order")
    void shouldReturnPublicationsWhenCallerIsAuthenticated() {
        // Given: the repository already orders by createdAt DESC
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(CALLER_ID));
        when(publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, CALLER_ID)).thenReturn(campaign);
        when(publicationRepository.findAllByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(newer, older));
        when(publicationLikeRepository.countVisibleByCampaignIdGroupedByPublication(eq(CAMPAIGN_ID),
                any(LocalDateTime.class))).thenReturn(List.of());
        when(publicationLikeRepository.findLikedPublicationIdsByCampaignIdAndUserId(CAMPAIGN_ID, CALLER_ID))
                .thenReturn(List.of());
        when(publicationMapper.toDTO(newer, 0L, false)).thenReturn(newerDto);
        when(publicationMapper.toDTO(older, 0L, false)).thenReturn(olderDto);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByCampaignId(CAMPAIGN_ID);

        // Then
        assertThat(result).containsExactly(newerDto, olderDto);

        InOrder inOrder = inOrder(authenticatedUserService, publicationValidationService, publicationRepository,
                publicationLikeRepository);
        inOrder.verify(authenticatedUserService).findAuthenticatedUserId();
        inOrder.verify(publicationValidationService).validateAndGetReadableCampaign(CAMPAIGN_ID, CALLER_ID);
        inOrder.verify(publicationRepository).findAllByCampaignId(CAMPAIGN_ID);
        inOrder.verify(publicationLikeRepository)
                .countVisibleByCampaignIdGroupedByPublication(eq(CAMPAIGN_ID), any(LocalDateTime.class));
        inOrder.verify(publicationLikeRepository).findLikedPublicationIdsByCampaignIdAndUserId(CAMPAIGN_ID, CALLER_ID);
    }

    @Test
    @DisplayName("Should validate the campaign with a null caller id when the caller is anonymous")
    void shouldValidateWithNullCallerWhenAnonymous() {
        // Given
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, null)).thenReturn(campaign);
        when(publicationRepository.findAllByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(newer));
        when(publicationLikeRepository.countVisibleByCampaignIdGroupedByPublication(eq(CAMPAIGN_ID),
                any(LocalDateTime.class))).thenReturn(List.of());
        when(publicationMapper.toDTO(newer, 0L, false)).thenReturn(newerDto);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByCampaignId(CAMPAIGN_ID);

        // Then
        assertThat(result).containsExactly(newerDto);
        verify(publicationValidationService).validateAndGetReadableCampaign(CAMPAIGN_ID, null);
    }

    @Test
    @DisplayName("Should return an empty list when the campaign has no publications")
    void shouldReturnEmptyListWhenCampaignHasNoPublications() {
        // Given
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, null)).thenReturn(campaign);
        when(publicationRepository.findAllByCampaignId(CAMPAIGN_ID)).thenReturn(Collections.emptyList());

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByCampaignId(CAMPAIGN_ID);

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(publicationMapper);
        verifyNoInteractions(publicationLikeRepository);
    }

    @Test
    @DisplayName("Should not query the repository when the campaign is not visible to the caller")
    void shouldNotQueryRepositoryWhenCampaignIsNotVisible() {
        // Given: a CLOSED campaign of someone else, a missing one or one of a deleted owner
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, null))
                .thenThrow(new ResourceNotFoundException("Campaign not found with id: " + CAMPAIGN_ID));

        // When & Then
        assertThatThrownBy(() -> publicationService.getPublicationsByCampaignId(CAMPAIGN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Campaign not found with id: " + CAMPAIGN_ID);

        verify(publicationRepository, never()).findAllByCampaignId(any());
        verifyNoInteractions(publicationMapper);
        verifyNoInteractions(publicationLikeRepository);
    }

    @Test
    @DisplayName("Should pass each publication its visible likes count, counted with the current time")
    void shouldPassEachPublicationItsLikesCount() {
        // Given
        Publication noLikes = Publication.builder().id(9L).campaignId(CAMPAIGN_ID).build();
        PublicationLikeRepository.PublicationLikeCountProjection newerCount = likeCount(11L, 3L);
        PublicationLikeRepository.PublicationLikeCountProjection olderCount = likeCount(10L, 1L);

        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, null)).thenReturn(campaign);
        when(publicationRepository.findAllByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(newer, older, noLikes));
        when(publicationLikeRepository.countVisibleByCampaignIdGroupedByPublication(eq(CAMPAIGN_ID),
                any(LocalDateTime.class))).thenReturn(List.of(newerCount, olderCount));
        stubOverloadReturningArguments();

        LocalDateTime before = LocalDateTime.now();

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByCampaignId(CAMPAIGN_ID);

        // Then
        assertThat(result).extracting(PublicationResponseDTO::getId).containsExactly(11L, 10L, 9L);
        assertThat(result).extracting(PublicationResponseDTO::getLikesCount).containsExactly(3L, 1L, 0L);

        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(publicationLikeRepository).countVisibleByCampaignIdGroupedByPublication(eq(CAMPAIGN_ID),
                nowCaptor.capture());
        assertThat(nowCaptor.getValue()).isAfterOrEqualTo(before).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should map a publication without a row in the count with zero likes")
    void shouldMapPublicationWithoutCountRowWithZeroLikes() {
        // Given: only the older publication has visible likes
        PublicationLikeRepository.PublicationLikeCountProjection olderCount = likeCount(10L, 2L);

        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(CALLER_ID));
        when(publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, CALLER_ID)).thenReturn(campaign);
        when(publicationRepository.findAllByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(newer, older));
        when(publicationLikeRepository.countVisibleByCampaignIdGroupedByPublication(eq(CAMPAIGN_ID),
                any(LocalDateTime.class))).thenReturn(List.of(olderCount));
        when(publicationLikeRepository.findLikedPublicationIdsByCampaignIdAndUserId(CAMPAIGN_ID, CALLER_ID))
                .thenReturn(List.of());
        when(publicationMapper.toDTO(newer, 0L, false)).thenReturn(newerDto);
        when(publicationMapper.toDTO(older, 2L, false)).thenReturn(olderDto);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByCampaignId(CAMPAIGN_ID);

        // Then
        assertThat(result).containsExactly(newerDto, olderDto);
        verify(publicationMapper).toDTO(newer, 0L, false);
        verify(publicationMapper).toDTO(older, 2L, false);
    }

    @Test
    @DisplayName("Should mark as liked by me only the publications the authenticated caller liked")
    void shouldMarkLikedByMeOnlyForCallerLikes() {
        // Given: the caller liked the older publication, someone else liked the newer one
        PublicationLikeRepository.PublicationLikeCountProjection newerCount = likeCount(11L, 1L);
        PublicationLikeRepository.PublicationLikeCountProjection olderCount = likeCount(10L, 1L);

        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(CALLER_ID));
        when(publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, CALLER_ID)).thenReturn(campaign);
        when(publicationRepository.findAllByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(newer, older));
        when(publicationLikeRepository.countVisibleByCampaignIdGroupedByPublication(eq(CAMPAIGN_ID),
                any(LocalDateTime.class))).thenReturn(List.of(newerCount, olderCount));
        when(publicationLikeRepository.findLikedPublicationIdsByCampaignIdAndUserId(CAMPAIGN_ID, CALLER_ID))
                .thenReturn(List.of(10L));
        stubOverloadReturningArguments();

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByCampaignId(CAMPAIGN_ID);

        // Then
        assertThat(result).extracting(PublicationResponseDTO::getId).containsExactly(11L, 10L);
        assertThat(result).extracting(PublicationResponseDTO::getLikedByMe).containsExactly(false, true);
        verify(publicationLikeRepository).findLikedPublicationIdsByCampaignIdAndUserId(CAMPAIGN_ID, CALLER_ID);
    }

    @Test
    @DisplayName("Should not look up the caller likes and map every publication as not liked when anonymous")
    void shouldNotLookUpCallerLikesWhenAnonymous() {
        // Given
        PublicationLikeRepository.PublicationLikeCountProjection newerCount = likeCount(11L, 4L);

        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, null)).thenReturn(campaign);
        when(publicationRepository.findAllByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(newer, older));
        when(publicationLikeRepository.countVisibleByCampaignIdGroupedByPublication(eq(CAMPAIGN_ID),
                any(LocalDateTime.class))).thenReturn(List.of(newerCount));
        stubOverloadReturningArguments();

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByCampaignId(CAMPAIGN_ID);

        // Then
        assertThat(result).extracting(PublicationResponseDTO::getLikesCount).containsExactly(4L, 0L);
        assertThat(result).extracting(PublicationResponseDTO::getLikedByMe).containsOnly(false);
        verify(publicationLikeRepository, never()).findLikedPublicationIdsByCampaignIdAndUserId(any(), any());
    }

    private static PublicationLikeRepository.PublicationLikeCountProjection likeCount(Long publicationId,
                                                                                      Long likesCount) {
        PublicationLikeRepository.PublicationLikeCountProjection row =
                mock(PublicationLikeRepository.PublicationLikeCountProjection.class);
        when(row.getPublicationId()).thenReturn(publicationId);
        when(row.getLikesCount()).thenReturn(likesCount);
        return row;
    }

    // The mapper echoes what the service passes it, so the result shows the likes of each publication
    private void stubOverloadReturningArguments() {
        when(publicationMapper.toDTO(any(Publication.class), anyLong(), anyBoolean())).thenAnswer(invocation -> {
            Publication publication = invocation.getArgument(0);
            return PublicationResponseDTO.builder()
                    .id(publication.getId())
                    .campaignId(publication.getCampaignId())
                    .likesCount(invocation.getArgument(1))
                    .likedByMe(invocation.getArgument(2))
                    .build();
        });
    }
}
