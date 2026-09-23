package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Publication;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.mapper.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.PublicationValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private final PublicationRepository publicationRepository;
    private final PublicationMapper publicationMapper;
    private final AuthenticatedUserService authenticatedUserService;
    private final PublicationValidationService publicationValidationService;

    public PublicationResponseDTO createPublication(CreatePublicationDTO dto) {
        Long authorId = authenticatedUserService.getAuthenticatedUserId();
        User user = publicationValidationService.validateAndGetAuthor(authorId);

        Publication publication = publicationMapper.toEntity(dto, user);
        publication = publicationRepository.save(publication);

        return publicationMapper.toDTO(publication);
    }

    public List<PublicationResponseDTO> getPublicationsByUserId(Long userId) {
        List<Publication> publications = publicationRepository.findAllByUser_Id(userId);
        return publications.stream()
                .map(publicationMapper::toDTO)
                .toList();
    }

    public void deletePublication(Long publicationId) {
        Long callerId = authenticatedUserService.getAuthenticatedUserId();
        Publication publication = publicationValidationService.validateAndGetOwnedPublication(publicationId, callerId);

        publicationRepository.delete(publication);
    }

}
