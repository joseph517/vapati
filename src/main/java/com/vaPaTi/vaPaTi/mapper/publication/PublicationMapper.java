package com.vaPaTi.vaPaTi.mapper.publication;

import com.vaPaTi.vaPaTi.dtos.publication.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.publication.PublicationLikeResponseDTO;
import com.vaPaTi.vaPaTi.dtos.publication.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PublicationMapper {

    // The author is always the campaign owner
    public Publication toEntity(CreatePublicationDTO dto, Campaign campaign) {
        Publication publication = new Publication();
        publication.setDescription(dto.getDescription());
        publication.setCampaign(campaign);
        publication.setUser(campaign.getUser());
        publication.setCreatedAt(LocalDateTime.now());
        publication.setUpdatedAt(LocalDateTime.now());
        return publication;
    }

    // A publication that was just created has no likes
    public PublicationResponseDTO toDTO(Publication publication) {
        return toDTO(publication, 0L, false);
    }

    public PublicationResponseDTO toDTO(Publication publication, long likesCount, boolean likedByMe) {
        PublicationResponseDTO dto = new PublicationResponseDTO();
        dto.setId(publication.getId());
        dto.setDescription(publication.getDescription());
        dto.setCreatedAt(publication.getCreatedAt());
        dto.setUpdatedAt(publication.getUpdatedAt());
        // The read-only campaignId is only loaded from the database: right after a save it is still null
        Campaign campaign = publication.getCampaign();
        dto.setCampaignId(campaign != null ? campaign.getId() : publication.getCampaignId());

        UserInfo userInfo = publication.getUser().getUserInfo();
        dto.setUserId(publication.getUser().getId());
        dto.setFirstName(userInfo.getFirstName());
        dto.setLastName(userInfo.getLastName());
        dto.setUserName(userInfo.getUserName());
        dto.setLikesCount(likesCount);
        dto.setLikedByMe(likedByMe);

        return dto;
    }

    public PublicationLikeResponseDTO toLikeResponseDTO(Long publicationId, long likesCount, boolean likedByMe) {
        return PublicationLikeResponseDTO.builder()
                .publicationId(publicationId)
                .likesCount(likesCount)
                .likedByMe(likedByMe)
                .build();
    }
}
