package com.vaPaTi.vaPaTi.validation.publication;

import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.publication.PublicationRepository;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
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
@DisplayName("PublicationValidationService - Visible Publication Tests")
class PublicationValidationServiceVisibleTest {

    private static final Long PUBLICATION_ID = 10L;
    private static final Long MISSING_PUBLICATION_ID = 999L;
    private static final Long CAMPAIGN_ID = 5L;
    private static final Long AUTHOR_ID = 1L;
    private static final Long CALLER_ID = 2L;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private CampaignValidationService campaignValidationService;

    @InjectMocks
    private PublicationValidationService publicationValidationService;

    private Publication publication;
    private Campaign campaign;
    private Goal goal;

    @BeforeEach
    void setUp() {
        User author = User.builder().id(AUTHOR_ID).build();
        goal = Goal.builder().status(CampaignStatus.ACTIVE).build();
        campaign = Campaign.builder().id(CAMPAIGN_ID).user(author).goal(goal).build();

        publication = Publication.builder()
                .id(PUBLICATION_ID)
                .description("Test publication")
                .user(author)
                .campaignId(CAMPAIGN_ID)
                .build();
    }

    @Nested
    @DisplayName("findVisiblePublication")
    class FindVisiblePublication {

        @Test
        @DisplayName("Should return empty when the publication does not exist, without looking at the campaign")
        void shouldReturnEmpty_whenPublicationDoesNotExist() {
            when(publicationRepository.findById(MISSING_PUBLICATION_ID)).thenReturn(Optional.empty());

            Optional<Publication> result = publicationValidationService.findVisiblePublication(MISSING_PUBLICATION_ID, CALLER_ID);

            assertThat(result).isEmpty();
            verify(campaignValidationService, never()).findVisibleCampaignById(any(), any());
        }

        @Test
        @DisplayName("Should return empty when the author was deleted, without looking at the campaign")
        void shouldReturnEmpty_whenAuthorIsDeleted() {
            publication.setUser(null);
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));

            Optional<Publication> result = publicationValidationService.findVisiblePublication(PUBLICATION_ID, CALLER_ID);

            assertThat(result).isEmpty();
            verify(campaignValidationService, never()).findVisibleCampaignById(any(), any());
        }

        @Test
        @DisplayName("Should return empty when the campaign is not visible to the caller")
        void shouldReturnEmpty_whenCampaignIsNotVisible() {
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.empty());

            Optional<Publication> result = publicationValidationService.findVisiblePublication(PUBLICATION_ID, CALLER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return the publication when the campaign is visible")
        void shouldReturnPublication_whenCampaignIsVisible() {
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.of(campaign));

            Optional<Publication> result = publicationValidationService.findVisiblePublication(PUBLICATION_ID, CALLER_ID);

            assertThat(result).containsSame(publication);
        }

        @Test
        @DisplayName("Should return the publication when the visible campaign is CLOSED (no status rule)")
        void shouldReturnPublication_whenVisibleCampaignIsClosed() {
            goal.setStatus(CampaignStatus.CLOSED);
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.of(campaign));

            Optional<Publication> result = publicationValidationService.findVisiblePublication(PUBLICATION_ID, CALLER_ID);

            assertThat(result).containsSame(publication);
        }

        @Test
        @DisplayName("Should return the publication to its author (no owner rule)")
        void shouldReturnPublication_whenCallerIsAuthor() {
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, AUTHOR_ID)).thenReturn(Optional.of(campaign));

            Optional<Publication> result = publicationValidationService.findVisiblePublication(PUBLICATION_ID, AUTHOR_ID);

            assertThat(result).containsSame(publication);
        }

        @Test
        @DisplayName("Should pass the publication's campaign id and the caller id to findVisibleCampaignById")
        void shouldPassCampaignIdAndCallerId_toFindVisibleCampaignById() {
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(any(), any())).thenReturn(Optional.of(campaign));
            ArgumentCaptor<Long> campaignIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> callerIdCaptor = ArgumentCaptor.forClass(Long.class);

            publicationValidationService.findVisiblePublication(PUBLICATION_ID, CALLER_ID);

            verify(campaignValidationService).findVisibleCampaignById(campaignIdCaptor.capture(), callerIdCaptor.capture());
            assertThat(campaignIdCaptor.getValue()).isEqualTo(CAMPAIGN_ID);
            assertThat(callerIdCaptor.getValue()).isEqualTo(CALLER_ID);
        }

        @Test
        @DisplayName("Should pass a null caller id (anonymous) to findVisibleCampaignById")
        void shouldPassNullCallerId_whenAnonymous() {
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, null)).thenReturn(Optional.of(campaign));

            Optional<Publication> result = publicationValidationService.findVisiblePublication(PUBLICATION_ID, null);

            assertThat(result).containsSame(publication);
        }
    }

    @Nested
    @DisplayName("validateAndGetVisiblePublication")
    class ValidateAndGetVisiblePublication {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the publication does not exist, without looking at the campaign")
        void shouldThrowNotFound_whenPublicationDoesNotExist() {
            when(publicationRepository.findById(MISSING_PUBLICATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicationValidationService.validateAndGetVisiblePublication(MISSING_PUBLICATION_ID, CALLER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Publication not found with ID: 999");
            verify(campaignValidationService, never()).findVisibleCampaignById(any(), any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the author was deleted, without looking at the campaign")
        void shouldThrowNotFound_whenAuthorIsDeleted() {
            publication.setUser(null);
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));

            assertThatThrownBy(() -> publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Publication not found with ID: 10");
            verify(campaignValidationService, never()).findVisibleCampaignById(any(), any());
        }

        @Test
        @DisplayName("Should throw the publication's 404, never the campaign's, when the campaign is not visible")
        void shouldThrowPublicationNotFound_whenCampaignIsNotVisible() {
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Publication not found with ID: 10")
                    .message().doesNotContain("Campaign not found with id");
            verify(campaignValidationService, never()).findVisibleCampaignByIdOrThrow(any(), any());
        }

        @Test
        @DisplayName("Should return the publication when the campaign is visible")
        void shouldReturnPublication_whenCampaignIsVisible() {
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.of(campaign));

            Publication result = publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID);

            assertThat(result).isSameAs(publication);
        }

        @Test
        @DisplayName("Should return the publication when the visible campaign is CLOSED (no status rule)")
        void shouldReturnPublication_whenVisibleCampaignIsClosed() {
            goal.setStatus(CampaignStatus.CLOSED);
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, CALLER_ID)).thenReturn(Optional.of(campaign));

            Publication result = publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID);

            assertThat(result).isSameAs(publication);
        }

        @Test
        @DisplayName("Should return the publication to its author (no owner rule)")
        void shouldReturnPublication_whenCallerIsAuthor() {
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(CAMPAIGN_ID, AUTHOR_ID)).thenReturn(Optional.of(campaign));

            Publication result = publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, AUTHOR_ID);

            assertThat(result).isSameAs(publication);
        }

        @Test
        @DisplayName("Should pass the publication's campaign id and the caller id to findVisibleCampaignById")
        void shouldPassCampaignIdAndCallerId_toFindVisibleCampaignById() {
            when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));
            when(campaignValidationService.findVisibleCampaignById(any(), any())).thenReturn(Optional.of(campaign));
            ArgumentCaptor<Long> campaignIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> callerIdCaptor = ArgumentCaptor.forClass(Long.class);

            publicationValidationService.validateAndGetVisiblePublication(PUBLICATION_ID, CALLER_ID);

            verify(campaignValidationService).findVisibleCampaignById(campaignIdCaptor.capture(), callerIdCaptor.capture());
            assertThat(campaignIdCaptor.getValue()).isEqualTo(CAMPAIGN_ID);
            assertThat(callerIdCaptor.getValue()).isEqualTo(CALLER_ID);
        }
    }
}
