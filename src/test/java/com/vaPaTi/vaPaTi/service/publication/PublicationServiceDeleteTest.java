package com.vaPaTi.vaPaTi.service.publication;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import com.vaPaTi.vaPaTi.entity.Publication;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.PublicationService;
import com.vaPaTi.vaPaTi.validation.PublicationValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publication Service - Delete Publication")
class PublicationServiceDeleteTest {

    private static final Long PUBLICATION_ID = 100L;
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private PublicationMapper publicationMapper;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private PublicationValidationService publicationValidationService;

    private PublicationService publicationService;

    private Publication mockPublication;

    @BeforeEach
    void setUp() {
        publicationService = new PublicationService(publicationRepository, publicationMapper,
                authenticatedUserService, publicationValidationService);

        LocalDateTime baseDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        User mockOwnerUser = User.builder()
                .id(OWNER_ID)
                .verified(true)
                .createdAt(baseDateTime)
                .userInfo(UserInfo.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .userName("johndoe")
                        .build())
                .build();

        mockPublication = Publication.builder()
                .id(PUBLICATION_ID)
                .description("Test publication")
                .user(mockOwnerUser)
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .build();
    }

    @Test
    @DisplayName("Should delete the publication when the caller is the owner")
    void shouldDeletePublicationWhenCallerIsOwner() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(publicationValidationService.validateAndGetOwnedPublication(PUBLICATION_ID, OWNER_ID))
                .thenReturn(mockPublication);

        // When & Then
        assertThatNoException().isThrownBy(() -> publicationService.deletePublication(PUBLICATION_ID));

        verify(publicationRepository, times(1)).delete(mockPublication);
        verifyNoMoreInteractions(publicationRepository);
        verifyNoInteractions(publicationMapper);
    }

    @Test
    @DisplayName("Should take the caller id from AuthenticatedUserService before validating ownership")
    void shouldTakeCallerIdFromAuthenticatedUserService() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(publicationValidationService.validateAndGetOwnedPublication(PUBLICATION_ID, OWNER_ID))
                .thenReturn(mockPublication);

        // When
        publicationService.deletePublication(PUBLICATION_ID);

        // Then - Verify exact sequence of operations
        InOrder inOrder = inOrder(authenticatedUserService, publicationValidationService, publicationRepository);
        inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
        inOrder.verify(publicationValidationService).validateAndGetOwnedPublication(PUBLICATION_ID, OWNER_ID);
        inOrder.verify(publicationRepository).delete(mockPublication);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("Should not delete when the caller is not the owner")
    void shouldNotDeleteWhenCallerIsNotOwner() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OTHER_USER_ID);
        when(publicationValidationService.validateAndGetOwnedPublication(PUBLICATION_ID, OTHER_USER_ID))
                .thenThrow(new ForbiddenActionException("You don't have permission to delete this publication"));

        // When & Then
        assertThatThrownBy(() -> publicationService.deletePublication(PUBLICATION_ID))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("You don't have permission to delete this publication");

        verify(publicationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should not delete when the publication does not exist")
    void shouldNotDeleteWhenPublicationDoesNotExist() {
        // Given
        Long nonExistentPublicationId = 999L;
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(publicationValidationService.validateAndGetOwnedPublication(nonExistentPublicationId, OWNER_ID))
                .thenThrow(new ResourceNotFoundException("Publication not found with ID: " + nonExistentPublicationId));

        // When & Then
        assertThatThrownBy(() -> publicationService.deletePublication(nonExistentPublicationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Publication not found with ID: " + nonExistentPublicationId);

        verify(publicationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should not delete when the author of the publication was deleted")
    void shouldNotDeleteWhenAuthorWasDeleted() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(publicationValidationService.validateAndGetOwnedPublication(PUBLICATION_ID, OWNER_ID))
                .thenThrow(new ResourceNotFoundException("Publication not found with ID: " + PUBLICATION_ID));

        // When & Then
        assertThatThrownBy(() -> publicationService.deletePublication(PUBLICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Publication not found with ID: " + PUBLICATION_ID);

        verify(publicationRepository, never()).delete(any());
    }
}
