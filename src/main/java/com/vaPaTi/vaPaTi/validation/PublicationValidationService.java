package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.entity.Publication;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicationValidationService {

    private static final String PUBLICATION_NOT_FOUND = "Publication not found with ID: ";

    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;

    public User validateAndGetAuthor(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    public Publication validateAndGetOwnedPublication(Long publicationId, Long userId) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException(PUBLICATION_NOT_FOUND + publicationId));

        // A publication whose author was soft-deleted is hidden (spec 18)
        if (publication.getUser() == null) {
            throw new ResourceNotFoundException(PUBLICATION_NOT_FOUND + publicationId);
        }

        if (!publication.getUser().getId().equals(userId)) {
            throw new ForbiddenActionException("You don't have permission to delete this publication");
        }

        return publication;
    }
}
