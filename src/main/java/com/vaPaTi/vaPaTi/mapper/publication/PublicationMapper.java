package com.vaPaTi.vaPaTi.mapper.publication;

import com.vaPaTi.vaPaTi.dtos.publication.CreatePublicationDTO;
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

    public PublicationResponseDTO toDTO(Publication publication) {
        PublicationResponseDTO dto = new PublicationResponseDTO();
        dto.setId(publication.getId());
        dto.setDescription(publication.getDescription());
        dto.setCreatedAt(publication.getCreatedAt());
        dto.setUpdatedAt(publication.getUpdatedAt());
        dto.setCampaignId(publication.getCampaignId());

        UserInfo userInfo = publication.getUser().getUserInfo();
        dto.setUserId(publication.getUser().getId());
        dto.setFirstName(userInfo.getFirstName());
        dto.setLastName(userInfo.getLastName());
        dto.setUserName(userInfo.getUserName());

        return dto;
    }
}
