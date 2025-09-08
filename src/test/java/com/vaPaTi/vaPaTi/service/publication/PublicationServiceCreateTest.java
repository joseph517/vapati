package com.vaPaTi.vaPaTi.service.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.vaPaTi.vaPaTi.dtos.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Publication;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.service.PublicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publication Service - Create Publication")
class PublicationServiceCreateTest {

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PublicationMapper publicationMapper;

    private PublicationService publicationService;

    private CreatePublicationDTO createPublicationDTO;
    private User mockUser;
    private UserInfo mockUserInfo;
    private Publication mockPublication;
    private Publication savedPublication;
    private PublicationResponseDTO expectedResponseDTO;

    @BeforeEach
    void setUp() {
        publicationService = new PublicationService(publicationRepository, userRepository, publicationMapper);

        // Setup test data
        createPublicationDTO = CreatePublicationDTO.builder()
                .userId(1L)
                .description("Test publication description")
                .build();

        mockUserInfo = UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        mockUser = User.builder()
                .id(1L)
                .active(true)
                .verified(true)
                .createdAt(LocalDateTime.now())
                .userInfo(mockUserInfo)
                .build();

        mockPublication = Publication.builder()
                .description("Test publication description")
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        savedPublication = Publication.builder()
                .id(100L)
                .description("Test publication description")
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        expectedResponseDTO = PublicationResponseDTO.builder()
                .id(100L)
                .description("Test publication description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();
    }

    @Test
    @DisplayName("Should successfully create publication when valid data is provided")
    void shouldSuccessfullyCreatePublicationWhenValidDataProvided() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(publicationMapper.toEntity(createPublicationDTO, mockUser)).thenReturn(mockPublication);
        when(publicationRepository.save(mockPublication)).thenReturn(savedPublication);
        when(publicationMapper.toDTO(savedPublication)).thenReturn(expectedResponseDTO);

        // When
        PublicationResponseDTO result = publicationService.createPublication(createPublicationDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getDescription()).isEqualTo("Test publication description");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getUserName()).isEqualTo("johndoe");

        // Verify interactions and order
        InOrder inOrder = inOrder(userRepository, publicationMapper, publicationRepository);
        inOrder.verify(userRepository, times(1)).findById(1L);
        inOrder.verify(publicationMapper, times(1)).toEntity(createPublicationDTO, mockUser);
        inOrder.verify(publicationRepository, times(1)).save(mockPublication);
        inOrder.verify(publicationMapper, times(1)).toDTO(savedPublication);

        verifyNoMoreInteractions(userRepository, publicationRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should throw MessageException when user does not exist")
    void shouldThrowMessageExceptionWhenUserDoesNotExist() {
        // Given
        Long nonExistentUserId = 999L;
        CreatePublicationDTO dtoWithInvalidUser = CreatePublicationDTO.builder()
                .userId(nonExistentUserId)
                .description("Test publication description")
                .build();

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> publicationService.createPublication(dtoWithInvalidUser))
                .isInstanceOf(MessageException.class)
                .hasMessage("User not found with ID: " + nonExistentUserId);

        // Verify interactions
        verify(userRepository, times(1)).findById(nonExistentUserId);
        verify(publicationMapper, never()).toEntity(any(), any());
        verify(publicationRepository, never()).save(any());
        verify(publicationMapper, never()).toDTO(any());

        verifyNoMoreInteractions(userRepository, publicationRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should handle null userId gracefully")
    void shouldHandleNullUserIdGracefully() {
        // Given
        CreatePublicationDTO dtoWithNullUserId = CreatePublicationDTO.builder()
                .userId(null)
                .description("Test publication description")
                .build();

        when(userRepository.findById(null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> publicationService.createPublication(dtoWithNullUserId))
                .isInstanceOf(MessageException.class)
                .hasMessage("User not found with ID: null");

        // Verify interactions
        verify(userRepository, times(1)).findById(null);
        verify(publicationMapper, never()).toEntity(any(), any());
        verify(publicationRepository, never()).save(any());
        verify(publicationMapper, never()).toDTO(any());
    }

    @Test
    @DisplayName("Should create publication with empty description when description is empty")
    void shouldCreatePublicationWithEmptyDescriptionWhenDescriptionIsEmpty() {
        // Given
        CreatePublicationDTO dtoWithEmptyDescription = CreatePublicationDTO.builder()
                .userId(1L)
                .description("")
                .build();

        Publication emptyDescriptionPublication = Publication.builder()
                .description("")
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Publication savedEmptyDescriptionPublication = Publication.builder()
                .id(101L)
                .description("")
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PublicationResponseDTO emptyDescriptionResponse = PublicationResponseDTO.builder()
                .id(101L)
                .description("")
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(publicationMapper.toEntity(dtoWithEmptyDescription, mockUser)).thenReturn(emptyDescriptionPublication);
        when(publicationRepository.save(emptyDescriptionPublication)).thenReturn(savedEmptyDescriptionPublication);
        when(publicationMapper.toDTO(savedEmptyDescriptionPublication)).thenReturn(emptyDescriptionResponse);

        // When
        PublicationResponseDTO result = publicationService.createPublication(dtoWithEmptyDescription);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEmpty();
        assertThat(result.getId()).isEqualTo(101L);

        // Verify all interactions occurred
        verify(userRepository, times(1)).findById(1L);
        verify(publicationMapper, times(1)).toEntity(dtoWithEmptyDescription, mockUser);
        verify(publicationRepository, times(1)).save(emptyDescriptionPublication);
        verify(publicationMapper, times(1)).toDTO(savedEmptyDescriptionPublication);
    }

    @Test
    @DisplayName("Should create publication with null description when description is null")
    void shouldCreatePublicationWithNullDescriptionWhenDescriptionIsNull() {
        // Given
        CreatePublicationDTO dtoWithNullDescription = CreatePublicationDTO.builder()
                .userId(1L)
                .description(null)
                .build();

        Publication nullDescriptionPublication = Publication.builder()
                .description(null)
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Publication savedNullDescriptionPublication = Publication.builder()
                .id(102L)
                .description(null)
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PublicationResponseDTO nullDescriptionResponse = PublicationResponseDTO.builder()
                .id(102L)
                .description(null)
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(publicationMapper.toEntity(dtoWithNullDescription, mockUser)).thenReturn(nullDescriptionPublication);
        when(publicationRepository.save(nullDescriptionPublication)).thenReturn(savedNullDescriptionPublication);
        when(publicationMapper.toDTO(savedNullDescriptionPublication)).thenReturn(nullDescriptionResponse);

        // When
        PublicationResponseDTO result = publicationService.createPublication(dtoWithNullDescription);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isNull();
        assertThat(result.getId()).isEqualTo(102L);

        // Verify all interactions occurred
        verify(userRepository, times(1)).findById(1L);
        verify(publicationMapper, times(1)).toEntity(dtoWithNullDescription, mockUser);
        verify(publicationRepository, times(1)).save(nullDescriptionPublication);
        verify(publicationMapper, times(1)).toDTO(savedNullDescriptionPublication);
    }

    @Test
    @DisplayName("Should handle very long description without issues")
    void shouldHandleVeryLongDescriptionWithoutIssues() {
        // Given
        String longDescription = "A".repeat(10000); // Very long description
        CreatePublicationDTO dtoWithLongDescription = CreatePublicationDTO.builder()
                .userId(1L)
                .description(longDescription)
                .build();

        Publication longDescriptionPublication = Publication.builder()
                .description(longDescription)
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Publication savedLongDescriptionPublication = Publication.builder()
                .id(103L)
                .description(longDescription)
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PublicationResponseDTO longDescriptionResponse = PublicationResponseDTO.builder()
                .id(103L)
                .description(longDescription)
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(publicationMapper.toEntity(dtoWithLongDescription, mockUser)).thenReturn(longDescriptionPublication);
        when(publicationRepository.save(longDescriptionPublication)).thenReturn(savedLongDescriptionPublication);
        when(publicationMapper.toDTO(savedLongDescriptionPublication)).thenReturn(longDescriptionResponse);

        // When
        PublicationResponseDTO result = publicationService.createPublication(dtoWithLongDescription);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).hasSize(10000);
        assertThat(result.getId()).isEqualTo(103L);

        // Verify all interactions occurred
        verify(userRepository, times(1)).findById(1L);
        verify(publicationMapper, times(1)).toEntity(dtoWithLongDescription, mockUser);
        verify(publicationRepository, times(1)).save(longDescriptionPublication);
        verify(publicationMapper, times(1)).toDTO(savedLongDescriptionPublication);
    }

    @Test
    @DisplayName("Should maintain correct interaction sequence when creating publication")
    void shouldMaintainCorrectInteractionSequenceWhenCreatingPublication() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(publicationMapper.toEntity(createPublicationDTO, mockUser)).thenReturn(mockPublication);
        when(publicationRepository.save(mockPublication)).thenReturn(savedPublication);
        when(publicationMapper.toDTO(savedPublication)).thenReturn(expectedResponseDTO);

        // When
        publicationService.createPublication(createPublicationDTO);

        // Then - Verify exact sequence of operations
        InOrder inOrder = inOrder(userRepository, publicationMapper, publicationRepository);

        // Step 1: Find user
        inOrder.verify(userRepository).findById(1L);

        // Step 2: Map DTO to entity
        inOrder.verify(publicationMapper).toEntity(createPublicationDTO, mockUser);

        // Step 3: Save publication
        inOrder.verify(publicationRepository).save(mockPublication);

        // Step 4: Map entity to response DTO
        inOrder.verify(publicationMapper).toDTO(savedPublication);

        // Ensure no additional interactions
        inOrder.verifyNoMoreInteractions();
    }

}
