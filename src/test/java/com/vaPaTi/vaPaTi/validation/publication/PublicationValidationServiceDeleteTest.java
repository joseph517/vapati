package com.vaPaTi.vaPaTi.validation.publication;

import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.publication.PublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicationValidationService - Delete Tests")
class PublicationValidationServiceDeleteTest {

    @Mock
    private PublicationRepository publicationRepository;

    @InjectMocks
    private PublicationValidationService publicationValidationService;

    private User owner;
    private Publication publication;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .build();

        publication = Publication.builder()
                .id(10L)
                .description("Test publication")
                .user(owner)
                .build();
    }

    @Nested
    @DisplayName("validateAndGetOwnedPublication")
    class ValidateAndGetOwnedPublication {

        @Test
        @DisplayName("Should return the publication to its owner")
        void shouldReturnPublication_whenCallerIsOwner() {
            when(publicationRepository.findById(10L)).thenReturn(Optional.of(publication));

            Publication result = publicationValidationService.validateAndGetOwnedPublication(10L, 1L);

            assertThat(result).isEqualTo(publication);
        }

        @Test
        @DisplayName("Should throw ForbiddenActionException to a third party")
        void shouldThrowForbidden_whenCallerIsNotOwner() {
            when(publicationRepository.findById(10L)).thenReturn(Optional.of(publication));

            assertThatThrownBy(() -> publicationValidationService.validateAndGetOwnedPublication(10L, 2L))
                    .isInstanceOf(ForbiddenActionException.class)
                    .hasMessage("You don't have permission to delete this publication");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the publication does not exist")
        void shouldThrowNotFound_whenPublicationDoesNotExist() {
            when(publicationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicationValidationService.validateAndGetOwnedPublication(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Publication not found with ID: 999");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the author was deleted")
        void shouldThrowNotFound_whenAuthorIsDeleted() {
            publication.setUser(null);
            when(publicationRepository.findById(10L)).thenReturn(Optional.of(publication));

            assertThatThrownBy(() -> publicationValidationService.validateAndGetOwnedPublication(10L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Publication not found with ID: 10");
        }
    }
}
