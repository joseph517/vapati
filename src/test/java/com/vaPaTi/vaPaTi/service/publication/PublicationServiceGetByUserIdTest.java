package com.vaPaTi.vaPaTi.service.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vaPaTi.vaPaTi.service.PublicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaPaTi.vaPaTi.dtos.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Publication;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.mapper.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publication Service - Get Publications By User ID")
class PublicationServiceGetByUserIdTest {

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PublicationMapper publicationMapper;

    private PublicationService publicationService;

    private User mockUser;
    private UserInfo mockUserInfo;
    private Publication publication1;
    private Publication publication2;
    private Publication publication3;
    private PublicationResponseDTO responseDTO1;
    private PublicationResponseDTO responseDTO2;
    private PublicationResponseDTO responseDTO3;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        publicationService = new PublicationService(publicationRepository, userRepository, publicationMapper);

        baseDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        // Setup user data
        mockUserInfo = UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        mockUser = User.builder()
                .id(1L)
                .active(true)
                .verified(true)
                .createdAt(baseDateTime)
                .userInfo(mockUserInfo)
                .build();

        // Setup publication entities
        publication1 = Publication.builder()
                .id(100L)
                .description("First publication")
                .user(mockUser)
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .build();

        publication2 = Publication.builder()
                .id(101L)
                .description("Second publication")
                .user(mockUser)
                .createdAt(baseDateTime.plusHours(1))
                .updatedAt(baseDateTime.plusHours(1))
                .build();

        publication3 = Publication.builder()
                .id(102L)
                .description("Third publication")
                .user(mockUser)
                .createdAt(baseDateTime.plusHours(2))
                .updatedAt(baseDateTime.plusHours(2))
                .build();

        // Setup response DTOs
        responseDTO1 = PublicationResponseDTO.builder()
                .id(100L)
                .description("First publication")
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        responseDTO2 = PublicationResponseDTO.builder()
                .id(101L)
                .description("Second publication")
                .createdAt(baseDateTime.plusHours(1))
                .updatedAt(baseDateTime.plusHours(1))
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        responseDTO3 = PublicationResponseDTO.builder()
                .id(102L)
                .description("Third publication")
                .createdAt(baseDateTime.plusHours(2))
                .updatedAt(baseDateTime.plusHours(2))
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();
    }

    @Test
    @DisplayName("Should return list of publications when user has multiple publications")
    void shouldReturnListOfPublicationsWhenUserHasMultiplePublications() {
        // Given
        Long userId = 1L;
        List<Publication> mockPublications = Arrays.asList(publication1, publication2, publication3);

        when(publicationRepository.findAllByUser_Id(userId)).thenReturn(mockPublications);
        when(publicationMapper.toDTO(publication1)).thenReturn(responseDTO1);
        when(publicationMapper.toDTO(publication2)).thenReturn(responseDTO2);
        when(publicationMapper.toDTO(publication3)).thenReturn(responseDTO3);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByUserId(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(0).getDescription()).isEqualTo("First publication");
        assertThat(result.get(1).getId()).isEqualTo(101L);
        assertThat(result.get(1).getDescription()).isEqualTo("Second publication");
        assertThat(result.get(2).getId()).isEqualTo(102L);
        assertThat(result.get(2).getDescription()).isEqualTo("Third publication");

        // Verify repository interaction
        verify(publicationRepository, times(1)).findAllByUser_Id(eq(userId));

        // Verify mapper interactions
        verify(publicationMapper, times(1)).toDTO(eq(publication1));
        verify(publicationMapper, times(1)).toDTO(eq(publication2));
        verify(publicationMapper, times(1)).toDTO(eq(publication3));

        verifyNoMoreInteractions(publicationRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should return single publication when user has only one publication")
    void shouldReturnSinglePublicationWhenUserHasOnlyOnePublication() {
        // Given
        Long userId = 1L;
        List<Publication> mockPublications = Collections.singletonList(publication1);

        when(publicationRepository.findAllByUser_Id(userId)).thenReturn(mockPublications);
        when(publicationMapper.toDTO(publication1)).thenReturn(responseDTO1);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByUserId(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(0).getDescription()).isEqualTo("First publication");
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        assertThat(result.get(0).getLastName()).isEqualTo("Doe");
        assertThat(result.get(0).getUserName()).isEqualTo("johndoe");

        // Verify interactions
        verify(publicationRepository, times(1)).findAllByUser_Id(eq(userId));
        verify(publicationMapper, times(1)).toDTO(eq(publication1));

        verifyNoMoreInteractions(publicationRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should return empty list when user has no publications")
    void shouldReturnEmptyListWhenUserHasNoPublications() {
        // Given
        Long userId = 1L;
        List<Publication> emptyPublications = Collections.emptyList();

        when(publicationRepository.findAllByUser_Id(userId)).thenReturn(emptyPublications);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByUserId(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        assertThat(result).hasSize(0);

        // Verify interactions
        verify(publicationRepository, times(1)).findAllByUser_Id(eq(userId));
        verify(publicationMapper, times(0)).toDTO(null);

        verifyNoMoreInteractions(publicationRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should handle null userId gracefully")
    void shouldHandleNullUserIdGracefully() {
        // Given
        Long nullUserId = null;
        List<Publication> emptyPublications = Collections.emptyList();

        when(publicationRepository.findAllByUser_Id(nullUserId)).thenReturn(emptyPublications);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByUserId(nullUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // Verify interactions
        verify(publicationRepository, times(1)).findAllByUser_Id(null);
        verify(publicationMapper, times(0)).toDTO(null);

        verifyNoMoreInteractions(publicationRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should handle non-existent user ID gracefully")
    void shouldHandleNonExistentUserIdGracefully() {
        // Given
        Long nonExistentUserId = 999L;
        List<Publication> emptyPublications = Collections.emptyList();

        when(publicationRepository.findAllByUser_Id(nonExistentUserId)).thenReturn(emptyPublications);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByUserId(nonExistentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // Verify interactions
        verify(publicationRepository, times(1)).findAllByUser_Id(eq(nonExistentUserId));
        verify(publicationMapper, times(0)).toDTO(null);

        verifyNoMoreInteractions(publicationRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should handle publications with null or empty descriptions")
    void shouldHandlePublicationsWithNullOrEmptyDescriptions() {
        // Given
        Long userId = 1L;

        Publication publicationWithNullDescription = Publication.builder()
                .id(200L)
                .description(null)
                .user(mockUser)
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .build();

        Publication publicationWithEmptyDescription = Publication.builder()
                .id(201L)
                .description("")
                .user(mockUser)
                .createdAt(baseDateTime)
                .updatedAt(baseDateTime)
                .build();

        List<Publication> publicationsWithEdgeCases = Arrays.asList(
                publicationWithNullDescription,
                publicationWithEmptyDescription
        );

        PublicationResponseDTO responseDTOWithNull = PublicationResponseDTO.builder()
                .id(200L)
                .description(null)
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        PublicationResponseDTO responseDTOWithEmpty = PublicationResponseDTO.builder()
                .id(201L)
                .description("")
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .build();

        when(publicationRepository.findAllByUser_Id(userId)).thenReturn(publicationsWithEdgeCases);
        when(publicationMapper.toDTO(publicationWithNullDescription)).thenReturn(responseDTOWithNull);
        when(publicationMapper.toDTO(publicationWithEmptyDescription)).thenReturn(responseDTOWithEmpty);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByUserId(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDescription()).isNull();
        assertThat(result.get(1).getDescription()).isEmpty();

        // Verify interactions
        verify(publicationRepository, times(1)).findAllByUser_Id(eq(userId));
        verify(publicationMapper, times(1)).toDTO(eq(publicationWithNullDescription));
        verify(publicationMapper, times(1)).toDTO(eq(publicationWithEmptyDescription));

        verifyNoMoreInteractions(publicationRepository, publicationMapper);
    }

    @Test
    @DisplayName("Should handle large number of publications efficiently")
    void shouldHandleLargeNumberOfPublicationsEfficiently() {
        // Given
        Long userId = 1L;
        List<Publication> largePublicationList = new ArrayList<>();
        List<PublicationResponseDTO> expectedResponseList = new ArrayList<>();

        // Create 1000 publications for stress testing
        for (int i = 0; i < 1000; i++) {
            Publication publication = Publication.builder()
                    .id((long) i)
                    .description("Publication " + i)
                    .user(mockUser)
                    .createdAt(baseDateTime.plusMinutes(i))
                    .updatedAt(baseDateTime.plusMinutes(i))
                    .build();

            PublicationResponseDTO responseDTO = PublicationResponseDTO.builder()
                    .id((long) i)
                    .description("Publication " + i)
                    .userId(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .userName("johndoe")
                    .build();

            largePublicationList.add(publication);
            expectedResponseList.add(responseDTO);

            when(publicationMapper.toDTO(publication)).thenReturn(responseDTO);
        }

        when(publicationRepository.findAllByUser_Id(userId)).thenReturn(largePublicationList);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByUserId(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1000);
        assertThat(result.get(0).getDescription()).isEqualTo("Publication 0");
        assertThat(result.get(999).getDescription()).isEqualTo("Publication 999");

        // Verify interactions
        verify(publicationRepository, times(1)).findAllByUser_Id(eq(userId));
        verify(publicationMapper, times(1000)).toDTO(org.mockito.ArgumentMatchers.any(Publication.class));

        verifyNoMoreInteractions(publicationRepository);
    }

    @Test
    @DisplayName("Should maintain correct interaction sequence when retrieving publications")
    void shouldMaintainCorrectInteractionSequenceWhenRetrievingPublications() {
        // Given
        Long userId = 1L;
        List<Publication> mockPublications = Arrays.asList(publication1, publication2);

        when(publicationRepository.findAllByUser_Id(userId)).thenReturn(mockPublications);
        when(publicationMapper.toDTO(publication1)).thenReturn(responseDTO1);
        when(publicationMapper.toDTO(publication2)).thenReturn(responseDTO2);

        // When
        publicationService.getPublicationsByUserId(userId);

        // Then - Verify exact sequence of operations
        InOrder inOrder = inOrder(publicationRepository, publicationMapper);

        // Step 1: Repository query
        inOrder.verify(publicationRepository).findAllByUser_Id(userId);

        // Step 2: Map first publication (order may vary due to stream processing)
        inOrder.verify(publicationMapper).toDTO(publication1);

        // Step 3: Map second publication
        inOrder.verify(publicationMapper).toDTO(publication2);

        // Ensure no additional interactions
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("Should preserve order of publications as returned by repository")
    void shouldPreserveOrderOfPublicationsAsReturnedByRepository() {
        // Given
        Long userId = 1L;
        // Deliberately order publications by creation time descending
        List<Publication> orderedPublications = Arrays.asList(publication3, publication1, publication2);

        when(publicationRepository.findAllByUser_Id(userId)).thenReturn(orderedPublications);
        when(publicationMapper.toDTO(publication3)).thenReturn(responseDTO3);
        when(publicationMapper.toDTO(publication1)).thenReturn(responseDTO1);
        when(publicationMapper.toDTO(publication2)).thenReturn(responseDTO2);

        // When
        List<PublicationResponseDTO> result = publicationService.getPublicationsByUserId(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        // Verify order is preserved
        assertThat(result.get(0).getId()).isEqualTo(102L); // publication3
        assertThat(result.get(1).getId()).isEqualTo(100L); // publication1
        assertThat(result.get(2).getId()).isEqualTo(101L); // publication2

        // Verify interactions
        verify(publicationRepository, times(1)).findAllByUser_Id(eq(userId));
        verify(publicationMapper, times(1)).toDTO(eq(publication3));
        verify(publicationMapper, times(1)).toDTO(eq(publication1));
        verify(publicationMapper, times(1)).toDTO(eq(publication2));

        verifyNoMoreInteractions(publicationRepository, publicationMapper);
    }

}
