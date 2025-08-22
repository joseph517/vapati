package com.vaPaTi.vaPaTi.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.*;

import com.vaPaTi.vaPaTi.dtos.CreateUserDTO;
import com.vaPaTi.vaPaTi.dtos.CreateUserInfoDTO;
import com.vaPaTi.vaPaTi.dtos.UserDTO;
import com.vaPaTi.vaPaTi.dtos.UserUserInfoRequestDTO;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.UserMapper;
import com.vaPaTi.vaPaTi.repository.RoleRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - createUser Method Tests")
class UserServiceCreateUserTest {

    @Mock
    private UserValidationService userValidationService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserService userService;

    private UserUserInfoRequestDTO validRequest;
    private CreateUserDTO createUserDTO;
    private CreateUserInfoDTO createUserInfoDTO;
    private List<Long> categoryIds;
    private List<Category> categories;
    private User mockUser;
    private UserInfo mockUserInfo;
    private Role mockRole;
    private User savedUser;
    private UserDTO expectedUserDTO;

    @BeforeEach
    void setUp() {
        // Setup category IDs
        categoryIds = Arrays.asList(1L, 2L, 3L);

        // Setup CreateUserDTO
        createUserDTO = new CreateUserDTO();
        createUserDTO.setCategoryIds(categoryIds);

        // Setup CreateUserInfoDTO
        createUserInfoDTO = new CreateUserInfoDTO();
        createUserInfoDTO.setFirstName("John");
        createUserInfoDTO.setLastName("Doe");
        createUserInfoDTO.setEmail("john.doe@example.com");
        createUserInfoDTO.setUserName("johndoe");
        createUserInfoDTO.setPassword("password123");
        createUserInfoDTO.setPhone("+1234567890");
        createUserInfoDTO.setDescription("Test user description");
        createUserInfoDTO.setProfilePicture("profile.jpg");

        // Setup valid request
        validRequest = new UserUserInfoRequestDTO();
        validRequest.setUser(createUserDTO);
        validRequest.setUserInfo(createUserInfoDTO);

        // Setup mock categories
        categories = Arrays.asList(
                Category.builder().id(1L).name("Category1").build(),
                Category.builder().id(2L).name("Category2").build(),
                Category.builder().id(3L).name("Category3").build()
        );

        // Setup mock user
        mockUser = User.builder()
                .id(1L)
                .active(true)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .userCategories(new HashSet<>())
                .bankAccounts(new HashSet<>())
                .build();

        // Setup mock user info
        mockUserInfo = UserInfo.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .userName("johndoe")
                .password("password123")
                .phone("+1234567890")
                .description("Test user description")
                .profilePicture("profile.jpg")
                .createdAt(LocalDateTime.now())
                .build();

        // Setup mock role
        mockRole = Role.builder()
                .id(1L)
                .name("USER")
                .build();

        // Setup saved user (what repository returns after save)
        savedUser = User.builder()
                .id(1L)
                .active(true)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .role(mockRole)
                .userInfo(mockUserInfo)
                .userCategories(new HashSet<>())
                .bankAccounts(new HashSet<>())
                .build();

        // Setup expected UserDTO
        expectedUserDTO = new UserDTO();
        expectedUserDTO.setId(1L);
        expectedUserDTO.setActive(true);
        expectedUserDTO.setVerified(false);
    }

    @Test
    @DisplayName("Should create user successfully with valid request")
    void shouldCreateUserSuccessfullyWithValidRequest() {
        // Given
        when(userValidationService.processCategories(categoryIds)).thenReturn(categories);
        when(userValidationService.createAndSetupUser(createUserDTO)).thenReturn(mockUser);
        when(userValidationService.createUserInfo(createUserInfoDTO)).thenReturn(mockUserInfo);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserDTO(savedUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.createUser(validRequest);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO.getId(), result.getId());
        assertEquals(expectedUserDTO.getActive(), result.getActive());
        assertEquals(expectedUserDTO.getVerified(), result.getVerified());;

        // Verify interaction order and calls
        InOrder inOrder = inOrder(userValidationService, roleRepository, userRepository, userMapper);

        inOrder.verify(userValidationService).validateCategoryLimit(categoryIds);
        inOrder.verify(userValidationService).processCategories(categoryIds);
        inOrder.verify(userValidationService).createAndSetupUser(createUserDTO);
        inOrder.verify(userValidationService).createUserCategoryRelations(mockUser, categories);
        inOrder.verify(userValidationService).createUserInfo(createUserInfoDTO);
        inOrder.verify(roleRepository).findByName("USER");
        inOrder.verify(userRepository).save(any(User.class));
        inOrder.verify(userMapper).toUserDTO(savedUser);

        // Verify that user was properly configured before saving
        verify(userRepository).save(argThat(user ->
                user.getRole().equals(mockRole) &&
                        user.getUserInfo().equals(mockUserInfo)
        ));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when request is null due to @NotNull validation")
    void shouldThrowIllegalArgumentExceptionWhenRequestIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(null)
        );

        // Verify it's Spring's Bean Validation exception
        assertThat(exception.getMessage())
                .contains("must not be null")
                .contains("request");

        // Verify no interactions with dependencies since validation happens before method execution
        verifyNoInteractions(userValidationService, roleRepository, userRepository, userMapper);
    }

    @Test
    @DisplayName("Should throw MessageException when USER role is not found")
    void shouldThrowMessageExceptionWhenUserRoleNotFound() {
        // Given
        when(userValidationService.processCategories(categoryIds)).thenReturn(categories);
        when(userValidationService.createAndSetupUser(createUserDTO)).thenReturn(mockUser);
        when(userValidationService.createUserInfo(createUserInfoDTO)).thenReturn(mockUserInfo);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> userService.createUser(validRequest)
        );

        assertEquals("Role not found", exception.getMessage());

        // Verify that save operations were never called
        verify(userRepository, never()).save(any(User.class));
        verify(userMapper, never()).toUserDTO(any(User.class));

        // Verify correct execution order up to the failure point
        InOrder inOrder = inOrder(userValidationService, roleRepository);
        inOrder.verify(userValidationService).validateCategoryLimit(categoryIds);
        inOrder.verify(userValidationService).processCategories(categoryIds);
        inOrder.verify(userValidationService).createAndSetupUser(createUserDTO);
        inOrder.verify(userValidationService).createUserCategoryRelations(mockUser, categories);
        inOrder.verify(userValidationService).createUserInfo(createUserInfoDTO);
        inOrder.verify(roleRepository).findByName("USER");
    }

    @Test
    @DisplayName("Should propagate MessageException from validateCategoryLimit")
    void shouldPropagateMessageExceptionFromValidateCategoryLimit() {
        // Given
        MessageException expectedException = new MessageException("Category limit exceeded");
        doThrow(expectedException).when(userValidationService).validateCategoryLimit(categoryIds);

        // When & Then
        MessageException exception = assertThrows(
                MessageException.class,
                () -> userService.createUser(validRequest)
        );

        assertEquals("Category limit exceeded", exception.getMessage());
        assertSame(expectedException, exception);

        // Verify that only validateCategoryLimit was called
        verify(userValidationService, only()).validateCategoryLimit(categoryIds);
        verifyNoInteractions(roleRepository, userRepository, userMapper);
    }

    @Test
    @DisplayName("Should propagate RuntimeException from processCategories")
    void shouldPropagateRuntimeExceptionFromProcessCategories() {
        // Given
        RuntimeException expectedException = new RuntimeException("Database connection error");
        when(userValidationService.processCategories(categoryIds)).thenThrow(expectedException);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(validRequest)
        );

        assertEquals("Database connection error", exception.getMessage());
        assertSame(expectedException, exception);

        // Verify execution stopped at processCategories
        verify(userValidationService).validateCategoryLimit(categoryIds);
        verify(userValidationService).processCategories(categoryIds);
        verify(userValidationService, never()).createAndSetupUser(any());
        verifyNoInteractions(roleRepository, userRepository, userMapper);
    }

    @Test
    @DisplayName("Should propagate exception from createAndSetupUser")
    void shouldPropagateExceptionFromCreateAndSetupUser() {
        // Given
        RuntimeException expectedException = new RuntimeException("User creation failed");
        when(userValidationService.processCategories(categoryIds)).thenReturn(categories);
        when(userValidationService.createAndSetupUser(createUserDTO)).thenThrow(expectedException);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(validRequest)
        );

        assertEquals("User creation failed", exception.getMessage());

        // Verify execution order up to failure point
        verify(userValidationService).validateCategoryLimit(categoryIds);
        verify(userValidationService).processCategories(categoryIds);
        verify(userValidationService).createAndSetupUser(createUserDTO);
        verify(userValidationService, never()).createUserCategoryRelations(any(), any());
    }

    @Test
    @DisplayName("Should propagate exception from createUserCategoryRelations")
    void shouldPropagateExceptionFromCreateUserCategoryRelations() {
        // Given
        RuntimeException expectedException = new RuntimeException("Category relation creation failed");
        when(userValidationService.processCategories(categoryIds)).thenReturn(categories);
        when(userValidationService.createAndSetupUser(createUserDTO)).thenReturn(mockUser);
        doThrow(expectedException).when(userValidationService)
                .createUserCategoryRelations(mockUser, categories);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(validRequest)
        );

        assertEquals("Category relation creation failed", exception.getMessage());

        // Verify execution order
        InOrder inOrder = inOrder(userValidationService);
        inOrder.verify(userValidationService).validateCategoryLimit(categoryIds);
        inOrder.verify(userValidationService).processCategories(categoryIds);
        inOrder.verify(userValidationService).createAndSetupUser(createUserDTO);
        inOrder.verify(userValidationService).createUserCategoryRelations(mockUser, categories);

        verify(userValidationService, never()).createUserInfo(any());
    }

    @Test
    @DisplayName("Should propagate exception from createUserInfo")
    void shouldPropagateExceptionFromCreateUserInfo() {
        // Given
        RuntimeException expectedException = new RuntimeException("User info creation failed");
        when(userValidationService.processCategories(categoryIds)).thenReturn(categories);
        when(userValidationService.createAndSetupUser(createUserDTO)).thenReturn(mockUser);
        when(userValidationService.createUserInfo(createUserInfoDTO)).thenThrow(expectedException);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(validRequest)
        );

        assertEquals("User info creation failed", exception.getMessage());

        // Verify that role lookup and save were never called
        verifyNoInteractions(roleRepository, userRepository, userMapper);
    }

    @Test
    @DisplayName("Should propagate exception from userRepository save operation")
    void shouldPropagateExceptionFromUserRepositorySave() {
        // Given
        RuntimeException expectedException = new RuntimeException("Database save failed");
        when(userValidationService.processCategories(categoryIds)).thenReturn(categories);
        when(userValidationService.createAndSetupUser(createUserDTO)).thenReturn(mockUser);
        when(userValidationService.createUserInfo(createUserInfoDTO)).thenReturn(mockUserInfo);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));
        when(userRepository.save(any(User.class))).thenThrow(expectedException);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(validRequest)
        );

        assertEquals("Database save failed", exception.getMessage());

        // Verify mapper was never called
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Should handle empty category list successfully")
    void shouldHandleEmptyCategoryListSuccessfully() {
        // Given
        CreateUserDTO userDTOWithEmptyCategories = new CreateUserDTO();
        userDTOWithEmptyCategories.setCategoryIds(new ArrayList<>());

        UserUserInfoRequestDTO requestWithEmptyCategories = new UserUserInfoRequestDTO();
        requestWithEmptyCategories.setUser(userDTOWithEmptyCategories);
        requestWithEmptyCategories.setUserInfo(createUserInfoDTO);

        when(userValidationService.processCategories(any())).thenReturn(new ArrayList<>());
        when(userValidationService.createAndSetupUser(userDTOWithEmptyCategories)).thenReturn(mockUser);
        when(userValidationService.createUserInfo(createUserInfoDTO)).thenReturn(mockUserInfo);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserDTO(savedUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.createUser(requestWithEmptyCategories);

        // Then
        assertNotNull(result);

        // Verify that empty list was passed to validation
        verify(userValidationService).validateCategoryLimit(Collections.emptyList());
        verify(userValidationService).processCategories(Collections.emptyList());
        verify(userValidationService).createUserCategoryRelations(eq(mockUser), eq(Collections.emptyList()));
    }

    @Test
    @DisplayName("Should ensure user and userInfo bidirectional relationship is set correctly")
    void shouldEnsureUserAndUserInfoBidirectionalRelationshipIsSetCorrectly() {
        // Given
        when(userValidationService.processCategories(categoryIds)).thenReturn(categories);
        when(userValidationService.createAndSetupUser(createUserDTO)).thenReturn(mockUser);
        when(userValidationService.createUserInfo(createUserInfoDTO)).thenReturn(mockUserInfo);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserDTO(savedUser)).thenReturn(expectedUserDTO);

        // When
        userService.createUser(validRequest);

        // Then
        // Verify that user was configured with proper relationships before saving
        verify(userRepository).save(argThat(user -> {
            // Check that role was set
            boolean roleSet = user.getRole() != null && user.getRole().equals(mockRole);

            // Check that userInfo was set
            boolean userInfoSet = user.getUserInfo() != null && user.getUserInfo().equals(mockUserInfo);

            return roleSet && userInfoSet;
        }));

        // Verify that bidirectional relationship method was called
        // Note: This assumes the setUserInfo method in User entity sets the bidirectional relationship
        // If you want to test the actual bidirectional setting, you might need to spy on the user object
    }


}
