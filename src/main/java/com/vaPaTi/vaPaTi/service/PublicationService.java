package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Publication;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
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

    public void deletePublication(Long publicationId, Long userId) {

        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Publication not found with ID: " + publicationId));

        if (publication.getUser().getId() == null || !publication.getUser().getId().equals(userId)) {
            throw new ForbiddenActionException("You don't have permission to delete this publication");
        }

        publicationRepository.delete(publication);
    }

}
