package com.vaPaTi.vaPaTi.service.publication;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.mapper.PublicationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaPaTi.vaPaTi.entity.Publication;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.service.PublicationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publication Service - Delete Publication")
class PublicationServiceDeleteTest {

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PublicationMapper publicationMapper;

    private PublicationService publicationService;

    private User mockOwnerUser;
    private User mockOtherUser;
    private UserInfo mockOwnerUserInfo;
    private UserInfo mockOtherUserInfo;
    private Publication mockPublication;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        publicationService = new PublicationService(publicationRepository, userRepository, publicationMapper);

        baseDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        // Setup owner user
        mockOwnerUserInfo = UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        mockOwnerUser = User.builder()
                .id(1L)
                .active(true)
                .verified(true)
                .createdAt(baseDateTime)
                .userInfo(mockOwnerUserInfo)
                .build();

        // Setup other user (not owner)
        mockOtherUserInfo = UserInfo.builder()
                .firstName("Jane")
                .lastName("Smith")
                .userName("janesmith")
                .build();

        mockOtherUser = User.builder()
                .id(2L)
                .active(true)
                .verified(true)
                .createdAt(baseDateTime)
                .userInfo(mockOtherUserInfo)
                .build();

        // Setup publication owned by mockOwnerUser
        mockPublication = Publication.builder()
                .id(100L)
                .description("Test publication")
                .user(mockOwnerUser)
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .build();
    }

    @Test
    @DisplayName("Should successfully delete publication when user is the owner")
    void shouldSuccessfullyDeletePublicationWhenUserIsOwner() {
        // Given
        Long publicationId = 100L;
        Long ownerId = 1L;

        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(mockPublication));

        // When & Then
        assertThatNoException().isThrownBy(() ->
                publicationService.deletePublication(publicationId, ownerId)
        );

        // Verify interactions and order
        InOrder inOrder = inOrder(publicationRepository);
        inOrder.verify(publicationRepository, times(1)).findById(eq(publicationId));
        inOrder.verify(publicationRepository, times(1)).delete(eq(mockPublication));

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when publication does not exist")
    void shouldThrowIllegalArgumentExceptionWhenPublicationDoesNotExist() {
        // Given
        Long nonExistentPublicationId = 999L;
        Long userId = 1L;

        when(publicationRepository.findById(nonExistentPublicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                publicationService.deletePublication(nonExistentPublicationId, userId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Publication not found with ID: " + nonExistentPublicationId);

        // Verify interactions
        verify(publicationRepository, times(1)).findById(eq(nonExistentPublicationId));
        verify(publicationRepository, never()).delete(null);

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when user is not the owner")
    void shouldThrowIllegalStateExceptionWhenUserIsNotOwner() {
        // Given
        Long publicationId = 100L;
        Long nonOwnerId = 2L; // Different from publication owner (ID: 1)

        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(mockPublication));

        // When & Then
        assertThatThrownBy(() ->
                publicationService.deletePublication(publicationId, nonOwnerId)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You don't have permission to delete this publication");

        // Verify interactions
        verify(publicationRepository, times(1)).findById(eq(publicationId));
        verify(publicationRepository, never()).delete(null);

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when publication ID is null")
    void shouldThrowIllegalArgumentExceptionWhenPublicationIdIsNull() {
        // Given
        Long nullPublicationId = null;
        Long userId = 1L;

        when(publicationRepository.findById(nullPublicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                publicationService.deletePublication(nullPublicationId, userId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Publication not found with ID: null");

        // Verify interactions
        verify(publicationRepository, times(1)).findById(null);
        verify(publicationRepository, never()).delete(null);

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when user ID is null")
    void shouldThrowIllegalStateExceptionWhenUserIdIsNull() {
        // Given
        Long publicationId = 100L;
        Long nullUserId = null;

        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(mockPublication));

        // When & Then
        assertThatThrownBy(() ->
                publicationService.deletePublication(publicationId, nullUserId)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You don't have permission to delete this publication");

        // Verify interactions
        verify(publicationRepository, times(1)).findById(eq(publicationId));
        verify(publicationRepository, never()).delete(null);

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should handle publication with null user gracefully")
    void shouldHandlePublicationWithNullUserGracefully() {
        // Given
        Long publicationId = 100L;
        Long userId = 1L;

        Publication publicationWithNullUser = Publication.builder()
                .id(100L)
                .description("Test publication")
                .user(null)
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .build();

        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publicationWithNullUser));

        // When & Then
        assertThatThrownBy(() ->
                publicationService.deletePublication(publicationId, userId)
        )
                .isInstanceOf(NullPointerException.class);

        // Verify interactions
        verify(publicationRepository, times(1)).findById(eq(publicationId));
        verify(publicationRepository, never()).delete(null);

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should handle publication with user having null ID")
    void shouldHandlePublicationWithUserHavingNullId() {
        // Given
        Long publicationId = 100L;
        Long userId = 1L;

        User userWithNullId = User.builder()
                .id(null)
                .active(true)
                .verified(true)
                .createdAt(baseDateTime)
                .userInfo(mockOwnerUserInfo)
                .build();

        Publication publicationWithNullUserId = Publication.builder()
                .id(100L)
                .description("Test publication")
                .user(userWithNullId)
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .build();

        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publicationWithNullUserId));

        // When & Then
        assertThatThrownBy(() ->
                publicationService.deletePublication(publicationId, userId)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You don't have permission to delete this publication");

        // Verify interactions
        verify(publicationRepository, times(1)).findById(eq(publicationId));
        verify(publicationRepository, never()).delete(null);

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should successfully delete publication when both IDs are the same but different objects")
    void shouldSuccessfullyDeletePublicationWhenBothIdsAreSameButDifferentObjects() {
        // Given
        Long publicationId = 100L;
        Long userId = 1L;
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(mockPublication));

        // When & Then
        assertThatNoException().isThrownBy(() ->
                publicationService.deletePublication(publicationId, userId)
        );

        // Verify interactions
        verify(publicationRepository, times(1)).findById(eq(publicationId));
        verify(publicationRepository, times(1)).delete(eq(mockPublication));

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should maintain correct interaction sequence when deleting publication")
    void shouldMaintainCorrectInteractionSequenceWhenDeletingPublication() {
        // Given
        Long publicationId = 100L;
        Long ownerId = 1L;

        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(mockPublication));

        // When
        publicationService.deletePublication(publicationId, ownerId);

        // Then - Verify exact sequence of operations
        InOrder inOrder = inOrder(publicationRepository);

        // Step 1: Find publication
        inOrder.verify(publicationRepository).findById(publicationId);

        // Step 2: Delete publication (after ownership validation)
        inOrder.verify(publicationRepository).delete(mockPublication);

        // Ensure no additional interactions
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("Should not call delete when publication is not found")
    void shouldNotCallDeleteWhenPublicationIsNotFound() {
        // Given
        Long nonExistentPublicationId = 999L;
        Long userId = 1L;

        when(publicationRepository.findById(nonExistentPublicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                publicationService.deletePublication(nonExistentPublicationId, userId)
        )
                .isInstanceOf(IllegalArgumentException.class);

        // Verify delete is never called
        verify(publicationRepository, times(1)).findById(eq(nonExistentPublicationId));
        verify(publicationRepository, never()).delete(org.mockito.ArgumentMatchers.any());

        verifyNoMoreInteractions(publicationRepository);
    }

    @Test
    @DisplayName("Should not call delete when user lacks permission")
    void shouldNotCallDeleteWhenUserLacksPermission() {
        // Given
        Long publicationId = 100L;
        Long unauthorizedUserId = 999L;

        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(mockPublication));

        // When & Then
        assertThatThrownBy(() ->
                publicationService.deletePublication(publicationId, unauthorizedUserId)
        )
                .isInstanceOf(IllegalStateException.class);

        // Verify delete is never called
        verify(publicationRepository, times(1)).findById(eq(publicationId));
        verify(publicationRepository, never()).delete(org.mockito.ArgumentMatchers.any());

        verifyNoMoreInteractions(publicationRepository);
    }

    @Test
    @DisplayName("Should handle edge case with zero IDs")
    void shouldHandleEdgeCaseWithZeroIds() {
        // Given
        Long zeroPublicationId = 0L;
        Long zeroUserId = 0L;

        User userWithZeroId = User.builder()
                .id(0L)
                .active(true)
                .verified(true)
                .createdAt(baseDateTime)
                .userInfo(mockOwnerUserInfo)
                .build();

        Publication publicationWithZeroId = Publication.builder()
                .id(0L)
                .description("Test publication with zero ID")
                .user(userWithZeroId)
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .build();

        when(publicationRepository.findById(zeroPublicationId)).thenReturn(Optional.of(publicationWithZeroId));

        // When & Then
        assertThatNoException().isThrownBy(() ->
                publicationService.deletePublication(zeroPublicationId, zeroUserId)
        );

        // Verify interactions
        verify(publicationRepository, times(1)).findById(eq(zeroPublicationId));
        verify(publicationRepository, times(1)).delete(eq(publicationWithZeroId));

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should handle large ID values correctly")
    void shouldHandleLargeIdValuesCorrectly() {
        // Given
        Long largePublicationId = Long.MAX_VALUE;
        Long largeUserId = Long.MAX_VALUE;

        User userWithLargeId = User.builder()
                .id(Long.MAX_VALUE)
                .active(true)
                .verified(true)
                .createdAt(baseDateTime)
                .userInfo(mockOwnerUserInfo)
                .build();

        Publication publicationWithLargeId = Publication.builder()
                .id(Long.MAX_VALUE)
                .description("Test publication with large ID")
                .user(userWithLargeId)
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .build();

        when(publicationRepository.findById(largePublicationId)).thenReturn(Optional.of(publicationWithLargeId));

        // When & Then
        assertThatNoException().isThrownBy(() ->
                publicationService.deletePublication(largePublicationId, largeUserId)
        );

        // Verify interactions
        verify(publicationRepository, times(1)).findById(eq(largePublicationId));
        verify(publicationRepository, times(1)).delete(eq(publicationWithLargeId));

        verifyNoMoreInteractions(publicationRepository, userRepository, publicationMapper);
    }

}
