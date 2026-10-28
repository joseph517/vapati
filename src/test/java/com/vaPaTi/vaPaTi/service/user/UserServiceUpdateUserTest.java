package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.dtos.user.SocialLinkDTO;
import com.vaPaTi.vaPaTi.dtos.user.UpdateUserDTO;
import com.vaPaTi.vaPaTi.dtos.user.UserDTO;
import com.vaPaTi.vaPaTi.entity.category.Category;
import com.vaPaTi.vaPaTi.entity.user.SocialPlatform;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.user.UserMapper;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.user.UserAccountValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserCategoryValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserProfileValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    private UserCategoryValidationService userCategoryValidationService;
    @Mock
    private UserAccountValidationService userAccountValidationService;
    @Mock
    private UserProfileValidationService userProfileValidationService;
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
                .verified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userCategories(new HashSet<>())
                .bankAccounts(new HashSet<>())
                .build();

        updateUserDTO = UpdateUserDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .categoryIds(List.of(1L, 2L))
                .build();

        expectedUserDTO = UserDTO.builder()
                .id(authenticatedUserId)
                .verified(false)
                .build();

        mockCategories = List.of(
                Category.builder().id(1L).name("Category1").build(),
                Category.builder().id(2L).name("Category2").build()
        );

        inOrder = inOrder(
                authenticatedUserService,
                userValidationService,
                userCategoryValidationService,
                userAccountValidationService,
                userProfileValidationService,
                userRepository,
                userMapper
        );
    }

    @Nested
    @DisplayName("Profile URL fields")
    class ProfileFields {

        @Test
        @DisplayName("Should apply the profile fields to the user's UserInfo right after updateUserInfo")
        void shouldApplyProfileFieldsAfterUpdateUserInfo() {
            // Given
            UserInfo userInfo = UserInfo.builder().id(10L).build();
            mockUser.setUserInfo(userInfo);
            UpdateUserDTO dto = UpdateUserDTO.builder().website("https://example.com").build();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
            when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
            when(userRepository.save(mockUser)).thenReturn(mockUser);
            when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

            // When
            userService.updateUser(dto);

            // Then
            inOrder.verify(userAccountValidationService).updateUserInfo(mockUser, dto);
            inOrder.verify(userProfileValidationService).applyProfileFields(same(userInfo), same(dto));
            inOrder.verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("Should not save when a profile field is invalid")
        void shouldNotSaveWhenProfileFieldIsInvalid() {
            // Given
            UserInfo userInfo = UserInfo.builder().id(10L).build();
            mockUser.setUserInfo(userInfo);
            UpdateUserDTO dto = UpdateUserDTO.builder().website("ftp://x.com").build();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
            when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
            doThrow(new MessageException("Website must be a valid http or https URL"))
                    .when(userProfileValidationService).applyProfileFields(userInfo, dto);

            // When & Then
            MessageException exception = assertThrows(MessageException.class, () -> userService.updateUser(dto));
            assertEquals("Website must be a valid http or https URL", exception.getMessage());
            verify(userRepository, never()).save(any());
            verifyNoInteractions(userCategoryValidationService, userMapper);
        }
    }

    @Nested
    @DisplayName("Social links")
    class SocialLinks {

        @BeforeEach
        void setUpUser() {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
            when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        }

        @Test
        @DisplayName("Should not replace the social links when socialLinks is null")
        void shouldNotReplaceSocialLinksWhenNull() {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder().firstName("John").build();
            when(userRepository.save(mockUser)).thenReturn(mockUser);
            when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

            // When
            userService.updateUser(dto);

            // Then
            verify(userProfileValidationService, never()).replaceSocialLinks(any(), any());
            verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("Should replace the social links with an empty list, after the categories and before saving")
        void shouldReplaceSocialLinksWhenEmptyList() {
            // Given
            List<SocialLinkDTO> links = List.of();
            UpdateUserDTO dto = UpdateUserDTO.builder().categoryIds(List.of(1L, 2L)).socialLinks(links).build();
            when(userCategoryValidationService.processCategories(dto.getCategoryIds())).thenReturn(mockCategories);
            when(userRepository.save(mockUser)).thenReturn(mockUser);
            when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

            // When
            userService.updateUser(dto);

            // Then
            inOrder.verify(userProfileValidationService).applyProfileFields(mockUser.getUserInfo(), dto);
            inOrder.verify(userCategoryValidationService).updateUserCategories(mockUser, mockCategories);
            inOrder.verify(userProfileValidationService).replaceSocialLinks(same(mockUser), same(links));
            inOrder.verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("Should not save when the social links are invalid")
        void shouldNotSaveWhenSocialLinksAreInvalid() {
            // Given
            List<SocialLinkDTO> links = List.of(SocialLinkDTO.builder().platform(SocialPlatform.X).url("https://evil.com").build());
            UpdateUserDTO dto = UpdateUserDTO.builder().socialLinks(links).build();
            doThrow(new MessageException("X link must point to x.com or twitter.com"))
                    .when(userProfileValidationService).replaceSocialLinks(mockUser, links);

            // When & Then
            MessageException exception = assertThrows(MessageException.class, () -> userService.updateUser(dto));
            assertEquals("X link must point to x.com or twitter.com", exception.getMessage());
            verify(userRepository, never()).save(any());
            verifyNoInteractions(userMapper);
        }
    }

    @Test
    @DisplayName("Should throw MessageException when DTO is null")
    void shouldThrowMessageExceptionWhenDtoIsNull() {
        // Given - DTO is null

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> userService.updateUser(null)
        );

        assertEquals("DTO cannot be null", exception.getMessage());

        // Verify no interactions with dependencies
        verifyNoInteractions(authenticatedUserService, userValidationService, userAccountValidationService, userRepository, userMapper);
    }

    @Test
    @DisplayName("Should successfully update user with all fields including categories")
    void shouldSuccessfullyUpdateUserWithAllFieldsIncludingCategories() {
        // Given
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userCategoryValidationService.processCategories(updateUserDTO.getCategoryIds())).thenReturn(mockCategories);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.updateUser(updateUserDTO);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO, result);

        // Verify method calls in order
        inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
        inOrder.verify(userValidationService).getUserById(authenticatedUserId);
        inOrder.verify(userValidationService).updateTimestamp(mockUser);
        inOrder.verify(userAccountValidationService).updateUserInfo(mockUser, updateUserDTO);
        inOrder.verify(userProfileValidationService).applyProfileFields(mockUser.getUserInfo(), updateUserDTO);
        inOrder.verify(userCategoryValidationService).validateCategoryLimit(updateUserDTO.getCategoryIds());
        inOrder.verify(userCategoryValidationService).processCategories(updateUserDTO.getCategoryIds());
        inOrder.verify(userCategoryValidationService).updateUserCategories(mockUser, mockCategories);
        inOrder.verify(userRepository).save(mockUser);
        inOrder.verify(userMapper).toUserDTO(mockUser);
    }

    @Test
    @DisplayName("Should successfully update user without categories when categoryIds is null")
    void shouldSuccessfullyUpdateUserWithoutCategoriesWhenCategoryIdsIsNull() {
        // Given
        UpdateUserDTO dtoWithoutCategories = UpdateUserDTO.builder()
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
        verify(userCategoryValidationService, never()).validateCategoryLimit(any());
        verify(userCategoryValidationService, never()).processCategories(any());
        verify(userCategoryValidationService, never()).updateUserCategories(any(), any());

        // Verify other methods are called
        verify(authenticatedUserService).getAuthenticatedUserId();
        verify(userValidationService).getUserById(authenticatedUserId);
        verify(userValidationService).updateTimestamp(mockUser);
        verify(userAccountValidationService).updateUserInfo(mockUser, dtoWithoutCategories);
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
        verify(userAccountValidationService, never()).updateUserInfo(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when category list is empty")
    void shouldThrowIllegalArgumentExceptionWhenCategoryListIsEmpty() {
        // Given
        UpdateUserDTO dtoWithEmptyCategories = UpdateUserDTO.builder()
                .firstName("John")
                .categoryIds(List.of()) // Empty list
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        doThrow(new IllegalArgumentException("User must have at least 1 category"))
                .when(userCategoryValidationService).validateCategoryLimit(List.of());

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
        verify(userAccountValidationService).updateUserInfo(mockUser, dtoWithEmptyCategories);
        verify(userCategoryValidationService).validateCategoryLimit(List.of());

        // Verify no calls after validation failure
        verify(userCategoryValidationService, never()).processCategories(any());
        verify(userCategoryValidationService, never()).updateUserCategories(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when category limit exceeds maximum")
    void shouldThrowIllegalArgumentExceptionWhenCategoryLimitExceedsMaximum() {
        // Given
        List<Long> tooManyCategoryIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L); // More than 6
        UpdateUserDTO dtoWithTooManyCategories = UpdateUserDTO.builder()
                .categoryIds(tooManyCategoryIds)
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        doThrow(new IllegalArgumentException("User cannot have more than 6 categories"))
                .when(userCategoryValidationService).validateCategoryLimit(tooManyCategoryIds);

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
        verify(userAccountValidationService).updateUserInfo(mockUser, dtoWithTooManyCategories);
        verify(userCategoryValidationService).validateCategoryLimit(tooManyCategoryIds);

        // Verify no calls after validation failure
        verify(userCategoryValidationService, never()).processCategories(any());
        verify(userCategoryValidationService, never()).updateUserCategories(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Should throw MessageException when some categories are not found")
    void shouldThrowMessageExceptionWhenSomeCategoriesAreNotFound() {
        // Given
        List<Long> categoryIds = List.of(1L, 999L); // 999L doesn't exist
        UpdateUserDTO dtoWithInvalidCategories = UpdateUserDTO.builder()
                .categoryIds(categoryIds)
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userCategoryValidationService.processCategories(categoryIds))
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
        verify(userAccountValidationService).updateUserInfo(mockUser, dtoWithInvalidCategories);
        verify(userCategoryValidationService).validateCategoryLimit(categoryIds);
        verify(userCategoryValidationService).processCategories(categoryIds);

        // Verify no calls after processCategories failure
        verify(userCategoryValidationService, never()).updateUserCategories(any(), any());
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
                .when(userAccountValidationService).updateUserInfo(mockUser, updateUserDTO);

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
        verify(userAccountValidationService).updateUserInfo(mockUser, updateUserDTO);

        // Verify no calls after updateUserInfo failure
        verify(userCategoryValidationService, never()).validateCategoryLimit(any());
        verify(userCategoryValidationService, never()).processCategories(any());
        verify(userCategoryValidationService, never()).updateUserCategories(any(), any());
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
                .categoryIds(singleCategoryId)
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userCategoryValidationService.processCategories(singleCategoryId)).thenReturn(singleCategory);
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
        inOrder.verify(userAccountValidationService).updateUserInfo(mockUser, dtoWithSingleCategory);
        inOrder.verify(userCategoryValidationService).validateCategoryLimit(singleCategoryId);
        inOrder.verify(userCategoryValidationService).processCategories(singleCategoryId);
        inOrder.verify(userCategoryValidationService).updateUserCategories(mockUser, singleCategory);
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
                .categoryIds(maxCategoryIds)
                .build();

        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
        when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        when(userCategoryValidationService.processCategories(maxCategoryIds)).thenReturn(maxCategories);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.updateUser(dtoWithMaxCategories);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO, result);

        // Verify all category operations are performed
        verify(userCategoryValidationService).validateCategoryLimit(maxCategoryIds);
        verify(userCategoryValidationService).processCategories(maxCategoryIds);
        verify(userCategoryValidationService).updateUserCategories(mockUser, maxCategories);
    }

    @Nested
    @DisplayName("Credential change (currentPassword and tokensValidAfter)")
    class CredentialChangeTests {

        @BeforeEach
        void setUpUser() {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
            when(userValidationService.getUserById(authenticatedUserId)).thenReturn(mockUser);
        }

        @Test
        @DisplayName("Without credential change: tokensValidAfter stays null")
        void withoutCredentialChange_ShouldNotSetTokensValidAfter() {
            UpdateUserDTO dto = UpdateUserDTO.builder().description("New description").build();
            when(userAccountValidationService.validateCredentialChange(mockUser, dto)).thenReturn(false);
            when(userRepository.save(mockUser)).thenReturn(mockUser);
            when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);

            userService.updateUser(dto);

            assertNull(mockUser.getTokensValidAfter());
            inOrder.verify(userAccountValidationService).validateCredentialChange(mockUser, dto);
            inOrder.verify(userValidationService).updateTimestamp(mockUser);
            inOrder.verify(userAccountValidationService).updateUserInfo(mockUser, dto);
            inOrder.verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("With credential change: sets tokensValidAfter before saving")
        void withCredentialChange_ShouldSetTokensValidAfter() {
            UpdateUserDTO dto = UpdateUserDTO.builder().password("N3wPassw0rd!").currentPassword("Passw0rd!").build();
            when(userAccountValidationService.validateCredentialChange(mockUser, dto)).thenReturn(true);
            when(userRepository.save(mockUser)).thenReturn(mockUser);
            when(userMapper.toUserDTO(mockUser)).thenReturn(expectedUserDTO);
            LocalDateTime before = LocalDateTime.now();

            userService.updateUser(dto);

            assertNotNull(mockUser.getTokensValidAfter());
            assertFalse(mockUser.getTokensValidAfter().isBefore(before));
            verify(userAccountValidationService).updateUserInfo(mockUser, dto);
            verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("Missing currentPassword: 400 required and no field is modified")
        void withoutCurrentPassword_ShouldNotModifyAnything() {
            UpdateUserDTO dto = UpdateUserDTO.builder().password("N3wPassw0rd!").description("x").build();
            LocalDateTime originalUpdatedAt = mockUser.getUpdatedAt();
            when(userAccountValidationService.validateCredentialChange(mockUser, dto))
                    .thenThrow(new MessageException("Current password is required to change email or password"));

            MessageException exception = assertThrows(MessageException.class, () -> userService.updateUser(dto));

            assertEquals("Current password is required to change email or password", exception.getMessage());
            assertNull(mockUser.getTokensValidAfter());
            assertEquals(originalUpdatedAt, mockUser.getUpdatedAt());
            verify(userValidationService, never()).updateTimestamp(any());
            verify(userAccountValidationService, never()).updateUserInfo(any(), any());
            verify(userCategoryValidationService, never()).updateUserCategories(any(), any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Wrong currentPassword: 400 incorrect and no field is modified")
        void withWrongCurrentPassword_ShouldNotModifyAnything() {
            UpdateUserDTO dto = UpdateUserDTO.builder().email("new@example.com").currentPassword("wrong").build();
            when(userAccountValidationService.validateCredentialChange(mockUser, dto))
                    .thenThrow(new MessageException("Current password is incorrect"));

            MessageException exception = assertThrows(MessageException.class, () -> userService.updateUser(dto));

            assertEquals("Current password is incorrect", exception.getMessage());
            assertNull(mockUser.getTokensValidAfter());
            verify(userAccountValidationService, never()).updateUserInfo(any(), any());
            verify(userRepository, never()).save(any());
        }
    }
}
