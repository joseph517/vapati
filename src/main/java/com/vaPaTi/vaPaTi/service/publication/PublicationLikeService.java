package com.vaPaTi.vaPaTi.service.publication;

import com.vaPaTi.vaPaTi.dtos.publication.PublicationLikeResponseDTO;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.publication.PublicationLike;
import com.vaPaTi.vaPaTi.mapper.publication.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.publication.PublicationLikeRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PublicationLikeService {

    private final PublicationLikeRepository publicationLikeRepository;
    private final PublicationValidationService publicationValidationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final PublicationMapper publicationMapper;

    // Not @Transactional on purpose: if a concurrent request inserts the same like first, this save hits uk_publication_like_publication_user and only its own transaction rolls back (the transaction manager also clears the open-in-view persistence context), so the method can still re-read the state and answer 200. Inside an outer transaction it would be marked rollback-only and end in 409
    public PublicationLikeResponseDTO likePublication(Long publicationId) {
        Long callerId = authenticatedUserService.getAuthenticatedUserId();
        Publication publication = publicationValidationService.validateAndGetVisiblePublication(publicationId, callerId);

        if (!publicationLikeRepository.existsByPublication_IdAndUser_Id(publicationId, callerId)) {
            try {
                // getReferenceById avoids a SELECT of the user, the JWT filter already loaded it
                publicationLikeRepository.save(PublicationLike.builder()
                        .publication(publication)
                        .user(userRepository.getReferenceById(callerId))
                        .build());
            } catch (DataIntegrityViolationException e) {
                // Another request gave the same like first: the final state is the same, so it isn't rethrown
            }
        }

        return buildResponse(publicationId, true);
    }

    // Only ids: after a rollback the loaded publication is no longer in the persistence context
    private PublicationLikeResponseDTO buildResponse(Long publicationId, boolean likedByMe) {
        return publicationMapper.toLikeResponseDTO(publicationId,
                publicationLikeRepository.countVisibleByPublicationId(publicationId, LocalDateTime.now()), likedByMe);
    }
}
