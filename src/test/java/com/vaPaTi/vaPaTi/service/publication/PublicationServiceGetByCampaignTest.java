package com.vaPaTi.vaPaTi.service.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaPaTi.vaPaTi.dtos.publication.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.publication.PublicationMapper;
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
                authenticatedUserService, publicationValidationService);

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
        when(publicationMapper.toDTO(newer)).thenReturn(newerDto);
        when(publicationMapper.toDTO(older)).thenReturn(olderDto);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByCampaignId(CAMPAIGN_ID);

        // Then
        assertThat(result).containsExactly(newerDto, olderDto);

        InOrder inOrder = inOrder(authenticatedUserService, publicationValidationService, publicationRepository);
        inOrder.verify(authenticatedUserService).findAuthenticatedUserId();
        inOrder.verify(publicationValidationService).validateAndGetReadableCampaign(CAMPAIGN_ID, CALLER_ID);
        inOrder.verify(publicationRepository).findAllByCampaignId(CAMPAIGN_ID);
    }

    @Test
    @DisplayName("Should validate the campaign with a null caller id when the caller is anonymous")
    void shouldValidateWithNullCallerWhenAnonymous() {
        // Given
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(publicationValidationService.validateAndGetReadableCampaign(CAMPAIGN_ID, null)).thenReturn(campaign);
        when(publicationRepository.findAllByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(newer));
        when(publicationMapper.toDTO(newer)).thenReturn(newerDto);

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
    }
}
