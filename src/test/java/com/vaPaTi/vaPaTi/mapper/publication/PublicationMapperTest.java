package com.vaPaTi.vaPaTi.mapper.publication;

import com.vaPaTi.vaPaTi.dtos.publication.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.publication.PublicationLikeResponseDTO;
import com.vaPaTi.vaPaTi.dtos.publication.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PublicationMapper Tests")
class PublicationMapperTest {

    private final PublicationMapper publicationMapper = new PublicationMapper();

    private User owner;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        UserInfo userInfo = new UserInfo();
        userInfo.setFirstName("Ana");
        userInfo.setLastName("Owner");
        userInfo.setUserName("campaign_owner");

        owner = new User();
        owner.setId(1L);
        owner.setUserInfo(userInfo);

        campaign = new Campaign();
        campaign.setId(2L);
        campaign.setUser(owner);
    }

    @Test
    @DisplayName("Should copy campaignId and the author data to the DTO")
    void toDTO_ShouldMapCampaignIdAndAuthor() {
        // Given
        Publication publication = Publication.builder()
                .id(10L).description("Progress update").user(owner).campaign(campaign).campaignId(2L)
                .build();

        // When
        PublicationResponseDTO dto = publicationMapper.toDTO(publication);

        // Then
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getDescription()).isEqualTo("Progress update");
        assertThat(dto.getCampaignId()).isEqualTo(2L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getUserName()).isEqualTo("campaign_owner");
    }

    @Test
    @DisplayName("Should keep campaignId from the read-only column when the campaign is deleted (relation null)")
    void toDTO_WithDeletedCampaign_ShouldUseReadOnlyCampaignId() {
        // Given
        Publication publication = Publication.builder()
                .id(10L).description("Progress update").user(owner).campaign(null).campaignId(2L)
                .build();

        // When
        PublicationResponseDTO dto = publicationMapper.toDTO(publication);

        // Then
        assertThat(dto.getCampaignId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should take campaignId from the campaign when the read-only column isn't loaded yet (right after a save)")
    void toDTO_RightAfterSave_ShouldTakeCampaignIdFromCampaign() {
        // Given: insertable = false, so the saved entity keeps campaignId null until it is reloaded
        Publication publication = Publication.builder()
                .id(10L).description("Progress update").user(owner).campaign(campaign).campaignId(null)
                .build();

        // When
        PublicationResponseDTO dto = publicationMapper.toDTO(publication);

        // Then
        assertThat(dto.getCampaignId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should copy likesCount and likedByMe and map the rest like toDTO(publication)")
    void toDTO_WithLikes_ShouldCopyLikeDataAndRestOfFields() {
        // Given
        Publication publication = Publication.builder()
                .id(10L).description("Progress update").user(owner).campaign(null).campaignId(2L)
                .build();

        // When
        PublicationResponseDTO dto = publicationMapper.toDTO(publication, 5L, true);

        // Then
        assertThat(dto.getLikesCount()).isEqualTo(5L);
        assertThat(dto.getLikedByMe()).isTrue();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getDescription()).isEqualTo("Progress update");
        assertThat(dto.getCampaignId()).isEqualTo(2L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getFirstName()).isEqualTo("Ana");
        assertThat(dto.getLastName()).isEqualTo("Owner");
        assertThat(dto.getUserName()).isEqualTo("campaign_owner");
    }

    @Test
    @DisplayName("Should take campaignId from the campaign in the overload when the read-only column isn't loaded")
    void toDTO_WithLikes_RightAfterSave_ShouldTakeCampaignIdFromCampaign() {
        // Given
        Publication publication = Publication.builder()
                .id(10L).user(owner).campaign(campaign).campaignId(null)
                .build();

        // When
        PublicationResponseDTO dto = publicationMapper.toDTO(publication, 1L, false);

        // Then
        assertThat(dto.getCampaignId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should return zero likes and likedByMe false for a publication without like data")
    void toDTO_WithoutLikeData_ShouldReturnZeroLikesAndNotLiked() {
        // Given
        Publication publication = Publication.builder()
                .id(10L).description("Progress update").user(owner).campaign(campaign).campaignId(2L)
                .build();

        // When
        PublicationResponseDTO dto = publicationMapper.toDTO(publication);

        // Then
        assertThat(dto.getLikesCount()).isZero();
        assertThat(dto.getLikedByMe()).isFalse();
    }

    @Test
    @DisplayName("Should copy the three values to the like response")
    void toLikeResponseDTO_ShouldCopyAllValues() {
        // When
        PublicationLikeResponseDTO dto = publicationMapper.toLikeResponseDTO(10L, 7L, true);

        // Then
        assertThat(dto.getPublicationId()).isEqualTo(10L);
        assertThat(dto.getLikesCount()).isEqualTo(7L);
        assertThat(dto.getLikedByMe()).isTrue();
    }

    @Test
    @DisplayName("Should set the campaign and take the author from the campaign owner")
    void toEntity_WithCampaign_ShouldSetCampaignAndOwnerAsAuthor() {
        // Given
        CreatePublicationDTO dto = CreatePublicationDTO.builder().description("Thanks to every donor").build();

        // When
        Publication publication = publicationMapper.toEntity(dto, campaign);

        // Then
        assertThat(publication.getDescription()).isEqualTo("Thanks to every donor");
        assertThat(publication.getCampaign()).isSameAs(campaign);
        assertThat(publication.getUser()).isSameAs(owner);
    }
}
