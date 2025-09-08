package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Publication;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final PublicationMapper publicationMapper;

    public PublicationResponseDTO createPublication(CreatePublicationDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new MessageException("User not found with ID: " + dto.getUserId()));

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
                .orElseThrow(() -> new IllegalArgumentException("Publication not found with ID: " + publicationId));

        if (publication.getUser().getId() == null || !publication.getUser().getId().equals(userId)) {
            throw new IllegalStateException("You don't have permission to delete this publication");
        }

        publicationRepository.delete(publication);
    }

}
