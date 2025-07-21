package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Publication;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PublicationMapper {

    public Publication toEntity(CreatePublicationDTO dto, User user) {
        Publication publication = new Publication();
        publication.setDescription(dto.getDescription());
        publication.setUser(user);
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

        UserInfo userInfo = publication.getUser().getUserInfo();
        dto.setUserId(publication.getUser().getId());
        dto.setFirstName(userInfo.getFirstName());
        dto.setLastName(userInfo.getLastName());
        dto.setUserName(userInfo.getUserName());

        return dto;
    }
}
