package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.dtos.UpdateUserDTO;
import com.vaPaTi.vaPaTi.dtos.UserDTO;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.UserMapper;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.UserService;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - updateUser Tests")
class UserServiceUpdateUserTest {

    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserService userService;

    private Long authenticatedUserId;
    private User mockUser;
    private UpdateUserDTO updateUserDTO;
    private UserDTO expectedUserDTO;
    private List<Category> mockCategories;
    private InOrder inOrder;

    @BeforeEach
    void setUp() {
        authenticatedUserId = 1L;

        mockUser = User.builder()
                .id(authenticatedUserId)
                .active(true)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userCategories(new HashSet<>())
                .bankAccounts(new HashSet<>())
                .build();

        updateUserDTO = UpdateUserDTO.builder()
                .active(false)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .categoryIds(List.of(1L, 2L))
                .build();

        expectedUserDTO = UserDTO.builder()
                .id(authenticatedUserId)
                .active(false)
                .verified(false)
                .build();

        mockCategories = List.of(
                Category.builder().id(1L).name("Category1").build(),
                Category.builder().id(2L).name("Category2").build()
        );

        inOrder = inOrder(
                authenticatedUserService,
                userValidationService,
                userRepository,
                userMapper
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when DTO is null")
    void shouldThrowIllegalArgumentExceptionWhenDtoIsNull() {
        // Given - DTO is null

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(null)
        );

        assertEquals("DTO cannot be null", exception.getMessage());

        // Verify no interactions with dependencies
        verifyNoInteractions(authenticatedUserService, userValidationService, userRepository, userMapper);
    }

    @Test
    @DisplayName("Should successfully update user with all fields including categories")
    void shouldSuccessfullyUpdateUserWithAllFieldsIncludingCategories() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userValidationService.processCategories(updateUserDTO.getCategoryIds())).thenReturn(mockCategories);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.updateUser(updateUserDTO);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO, result);
        assertEquals(false, mockUser.isActive()); // Verify user active status was updated

        // Verify method calls in order
        inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
        inOrder.verify(userValidationService).getUserById(authenticatedUserId);
        inOrder.verify(userValidationService).updateTimestamp(mockUser);
        inOrder.verify(userValidationService).updateUserInfo(mockUser, updateUserDTO);
        inOrder.verify(userValidationService).validateCategoryLimit(updateUserDTO.getCategoryIds());
        inOrder.verify(userValidationService).processCategories(updateUserDTO.getCategoryIds());
        inOrder.verify(userValidationService).updateUserCategories(mockUser, mockCategories);
        inOrder.verify(userRepository).save(mockUser);
        inOrder.verify(userMapper).toUserDTO(mockUser);
    }

    @Test
    @DisplayName("Should successfully update user without categories when categoryIds is null")
    void shouldSuccessfullyUpdateUserWithoutCategoriesWhenCategoryIdsIsNull() {
        // Given
        UpdateUserDTO dtoWithoutCategories = UpdateUserDTO.builder()
                .active(false)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .categoryIds(null) // No categories
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.updateUser(dtoWithoutCategories);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO, result);

        // Verify category-related methods are not called
        verify(userValidationService, never()).validateCategoryLimit(any());
        verify(userValidationService, never()).processCategories(any());
        verify(userValidationService, never()).updateUserCategories(any(), any());

        // Verify other methods are called
        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userValidationService).getUserById(authenticatedUserId);
        verify(userValidationService).updateTimestamp(mockUser);
        verify(userValidationService).updateUserInfo(mockUser, dtoWithoutCategories);
        verify(userRepository).save(mockUser);
        verify(userMapper).toUserDTO(mockUser);
    }

    @Test
    @DisplayName("Should successfully update user with active field null")
    void shouldSuccessfullyUpdateUserWithActiveFieldNull() {
        // Given
        boolean originalActiveStatus = true;
        mockUser.setActive(originalActiveStatus);

        UpdateUserDTO dtoWithNullActive = UpdateUserDTO.builder()
                .active(null) // Active is null - should not change
                .firstName("John")
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.updateUser(dtoWithNullActive);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO, result);
        assertEquals(originalActiveStatus, mockUser.isActive()); // Should remain unchanged

        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userValidationService).getUserById(authenticatedUserId);
        verify(userValidationService).updateTimestamp(mockUser);
        verify(userValidationService).updateUserInfo(mockUser, dtoWithNullActive);
        verify(userRepository).save(mockUser);
        verify(userMapper).toUserDTO(mockUser);
    }

    @Test
    @DisplayName("Should throw MessageException when user is not found")
    void shouldThrowMessageExceptionWhenUserIsNotFound() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId))
                .thenThrow(new MessageException("User not found"));

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> userService.updateUser(updateUserDTO)
        );

        assertEquals("User not found", exception.getMessage());

        // Verify no further interactions after getUserById fails
        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userValidationService).getUserById(authenticatedUserId);
        verify(userValidationService, never()).updateTimestamp(any());
        verify(userValidationService, never()).updateUserInfo(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when category list is empty")
    void shouldThrowIllegalArgumentExceptionWhenCategoryListIsEmpty() {
        // Given
        UpdateUserDTO dtoWithEmptyCategories = UpdateUserDTO.builder()
                .active(false)
                .firstName("John")
                .categoryIds(List.of()) // Empty list
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        doThrow(new IllegalArgumentException("User must have at least 1 category"))
                .when(userValidationService).validateCategoryLimit(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(dtoWithEmptyCategories)
        );

        assertEquals("User must have at least 1 category", exception.getMessage());

        // Verify method calls up to validation failure
        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userValidationService).getUserById(authenticatedUserId);
        verify(userValidationService).updateTimestamp(mockUser);
        verify(userValidationService).updateUserInfo(mockUser, dtoWithEmptyCategories);
        verify(userValidationService).validateCategoryLimit(List.of());

        // Verify no calls after validation failure
        verify(userValidationService, never()).processCategories(any());
        verify(userValidationService, never()).updateUserCategories(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when category limit exceeds maximum")
    void shouldThrowIllegalArgumentExceptionWhenCategoryLimitExceedsMaximum() {
        // Given
        List<Long> tooManyCategoryIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L); // More than 6
        UpdateUserDTO dtoWithTooManyCategories = UpdateUserDTO.builder()
                .active(false)
                .categoryIds(tooManyCategoryIds)
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        doThrow(new IllegalArgumentException("User cannot have more than 6 categories"))
                .when(userValidationService).validateCategoryLimit(tooManyCategoryIds);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(dtoWithTooManyCategories)
        );

        assertEquals("User cannot have more than 6 categories", exception.getMessage());

        // Verify method calls up to validation failure
        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userValidationService).getUserById(authenticatedUserId);
        verify(userValidationService).updateTimestamp(mockUser);
        verify(userValidationService).updateUserInfo(mockUser, dtoWithTooManyCategories);
        verify(userValidationService).validateCategoryLimit(tooManyCategoryIds);

        // Verify no calls after validation failure
        verify(userValidationService, never()).processCategories(any());
        verify(userValidationService, never()).updateUserCategories(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Should throw MessageException when some categories are not found")
    void shouldThrowMessageExceptionWhenSomeCategoriesAreNotFound() {
        // Given
        List<Long> categoryIds = List.of(1L, 999L); // 999L doesn't exist
        UpdateUserDTO dtoWithInvalidCategories = UpdateUserDTO.builder()
                .active(false)
                .categoryIds(categoryIds)
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userValidationService.processCategories(categoryIds))
                .thenThrow(new MessageException("Categories not found: [999]"));

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> userService.updateUser(dtoWithInvalidCategories)
        );

        assertEquals("Categories not found: [999]", exception.getMessage());

        // Verify method calls up to processCategories failure
        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userValidationService).getUserById(authenticatedUserId);
        verify(userValidationService).updateTimestamp(mockUser);
        verify(userValidationService).updateUserInfo(mockUser, dtoWithInvalidCategories);
        verify(userValidationService).validateCategoryLimit(categoryIds);
        verify(userValidationService).processCategories(categoryIds);

        // Verify no calls after processCategories failure
        verify(userValidationService, never()).updateUserCategories(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Should handle exception during updateUserInfo")
    void shouldHandleExceptionDuringUpdateUserInfo() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        doThrow(new MessageException("Email already exists"))
                .when(userValidationService).updateUserInfo(mockUser, updateUserDTO);

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> userService.updateUser(updateUserDTO)
        );

        assertEquals("Email already exists", exception.getMessage());

        // Verify method calls up to updateUserInfo failure
        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userValidationService).getUserById(authenticatedUserId);
        verify(userValidationService).updateTimestamp(mockUser);
        verify(userValidationService).updateUserInfo(mockUser, updateUserDTO);

        // Verify no calls after updateUserInfo failure
        verify(userValidationService, never()).validateCategoryLimit(any());
        verify(userValidationService, never()).processCategories(any());
        verify(userValidationService, never()).updateUserCategories(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Should successfully update user with single category")
    void shouldSuccessfullyUpdateUserWithSingleCategory() {
        // Given
        List<Long> singleCategoryId = List.of(1L);
        List<Category> singleCategory = List.of(Category.builder().id(1L).name("Category1").build());

        UpdateUserDTO dtoWithSingleCategory = UpdateUserDTO.builder()
                .active(true)
                .categoryIds(singleCategoryId)
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userValidationService.processCategories(singleCategoryId)).thenReturn(singleCategory);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.updateUser(dtoWithSingleCategory);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO, result);

        // Verify all methods called correctly
        inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
        inOrder.verify(userValidationService).getUserById(authenticatedUserId);
        inOrder.verify(userValidationService).updateTimestamp(mockUser);
        inOrder.verify(userValidationService).updateUserInfo(mockUser, dtoWithSingleCategory);
        inOrder.verify(userValidationService).validateCategoryLimit(singleCategoryId);
        inOrder.verify(userValidationService).processCategories(singleCategoryId);
        inOrder.verify(userValidationService).updateUserCategories(mockUser, singleCategory);
        inOrder.verify(userRepository).save(mockUser);
        inOrder.verify(userMapper).toUserDTO(mockUser);
    }

    @Test
    @DisplayName("Should successfully update user with maximum allowed categories")
    void shouldSuccessfullyUpdateUserWithMaximumAllowedCategories() {
        // Given
        List<Long> maxCategoryIds = List.of(1L, 2L, 3L, 4L, 5L, 6L); // Exactly 6 categories
        List<Category> maxCategories = maxCategoryIds.stream()
                .map(id -> Category.builder().id(id).name("Category" + id).build())
                .toList();

        UpdateUserDTO dtoWithMaxCategories = UpdateUserDTO.builder()
                .active(true)
                .categoryIds(maxCategoryIds)
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userValidationService.processCategories(maxCategoryIds)).thenReturn(maxCategories);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.updateUser(dtoWithMaxCategories);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO, result);

        // Verify all category operations are performed
        verify(userValidationService).validateCategoryLimit(maxCategoryIds);
        verify(userValidationService).processCategories(maxCategoryIds);
        verify(userValidationService).updateUserCategories(mockUser, maxCategories);
    }

    @Test
    @DisplayName("Should update user with minimal DTO containing only active field")
    void shouldUpdateUserWithMinimalDtoContainingOnlyActiveField() {
        // Given
        UpdateUserDTO minimalDto = UpdateUserDTO.builder()
                .active(false)
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.updateUser(minimalDto);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO, result);
        assertEquals(false, mockUser.isActive());

        // Verify no category operations
        verify(userValidationService, never()).validateCategoryLimit(any());
        verify(userValidationService, never()).processCategories(any());
        verify(userValidationService, never()).updateUserCategories(any(), any());

        // Verify core operations
        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userValidationService).getUserById(authenticatedUserId);
        verify(userValidationService).updateTimestamp(mockUser);
        verify(userValidationService).updateUserInfo(mockUser, minimalDto);
        verify(userRepository).save(mockUser);
        verify(userMapper).toUserDTO(mockUser);
    }

}
