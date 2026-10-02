package com.vaPaTi.vaPaTi.service.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import com.vaPaTi.vaPaTi.dtos.publication.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.publication.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.publication.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.publication.PublicationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publication Service - Create Publication")
class PublicationServiceCreateTest {

    private static final Long CAMPAIGN_ID = 5L;
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private PublicationMapper publicationMapper;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private PublicationValidationService publicationValidationService;

    private PublicationService publicationService;

    private CreatePublicationDTO createPublicationDTO;
    private Campaign campaign;
    private Publication mockPublication;
    private Publication savedPublication;
    private PublicationResponseDTO expectedResponseDTO;

    @BeforeEach
    void setUp() {
        publicationService = new PublicationService(publicationRepository, publicationMapper,
                authenticatedUserService, publicationValidationService);

        createPublicationDTO = CreatePublicationDTO.builder()
                .description("Test publication description")
                .build();

        User owner = User.builder()
                .id(OWNER_ID)
                .verified(true)
                .createdAt(LocalDateTime.now())
                .userInfo(UserInfo.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .userName("johndoe")
                        .build())
                .build();

        campaign = Campaign.builder()
                .id(CAMPAIGN_ID)
                .user(owner)
                .build();

        mockPublication = Publication.builder()
                .description("Test publication description")
                .user(owner)
                .campaign(campaign)
                .build();

        savedPublication = Publication.builder()
                .id(100L)
                .description("Test publication description")
                .user(owner)
                .campaign(campaign)
                .campaignId(CAMPAIGN_ID)
                .createdAt(LocalDateTime.now())
                .build();

        expectedResponseDTO = PublicationResponseDTO.builder()
                .id(100L)
                .description("Test publication description")
                .campaignId(CAMPAIGN_ID)
                .userId(OWNER_ID)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();
    }

    @Test
    @DisplayName("Should validate the campaign with the token user, map with the campaign, save and map the response, in that order")
    void shouldCreatePublicationInCampaignWhenCallerIsOwner() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, OWNER_ID)).thenReturn(campaign);
        when(publicationMapper.toEntity(createPublicationDTO, campaign)).thenReturn(mockPublication);
        when(publicationRepository.save(mockPublication)).thenReturn(savedPublication);
        when(publicationMapper.toDTO(savedPublication)).thenReturn(expectedResponseDTO);

        // When
        PublicationResponseDTO result = publicationService.createPublication(CAMPAIGN_ID, createPublicationDTO);

        // Then
        assertThat(result).isSameAs(expectedResponseDTO);
        assertThat(result.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(result.getUserId()).isEqualTo(OWNER_ID);

        InOrder inOrder = inOrder(authenticatedUserService, publicationValidationService,
                publicationMapper, publicationRepository);
        inOrder.verify(authenticatedUserService, times(1)).getAuthenticatedUserId();
        inOrder.verify(publicationValidationService, times(1)).validateAndGetPublishableCampaign(CAMPAIGN_ID, OWNER_ID);
        inOrder.verify(publicationMapper, times(1)).toEntity(createPublicationDTO, campaign);
        inOrder.verify(publicationRepository, times(1)).save(mockPublication);
        inOrder.verify(publicationMapper, times(1)).toDTO(savedPublication);

        verifyNoMoreInteractions(authenticatedUserService, publicationValidationService,
                publicationRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should not save when the campaign is not visible to the caller (404)")
    void shouldNotSaveWhenCampaignIsNotVisible() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OTHER_USER_ID);
        when(publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, OTHER_USER_ID))
                .thenThrow(new ResourceNotFoundException("Campaign not found with id: " + CAMPAIGN_ID));

        // When & Then
        assertThatThrownBy(() -> publicationService.createPublication(CAMPAIGN_ID, createPublicationDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Campaign not found with id: " + CAMPAIGN_ID);

        verify(publicationMapper, never()).toEntity(any(), any());
        verify(publicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not save when the caller is not the campaign owner (403)")
    void shouldNotSaveWhenCallerIsNotOwner() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OTHER_USER_ID);
        when(publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, OTHER_USER_ID))
                .thenThrow(new ForbiddenActionException("Only the campaign owner can publish in it"));

        // When & Then
        assertThatThrownBy(() -> publicationService.createPublication(CAMPAIGN_ID, createPublicationDTO))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only the campaign owner can publish in it");

        verify(publicationMapper, never()).toEntity(any(), any());
        verify(publicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not save when the campaign is CLOSED (400)")
    void shouldNotSaveWhenCampaignIsClosed() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(publicationValidationService.validateAndGetPublishableCampaign(CAMPAIGN_ID, OWNER_ID))
                .thenThrow(new MessageException("Cannot publish in a closed campaign"));

        // When & Then
        assertThatThrownBy(() -> publicationService.createPublication(CAMPAIGN_ID, createPublicationDTO))
                .isExactlyInstanceOf(MessageException.class)
                .hasMessage("Cannot publish in a closed campaign");

        verify(publicationMapper, never()).toEntity(any(), any());
        verify(publicationRepository, never()).save(any());
    }
}
