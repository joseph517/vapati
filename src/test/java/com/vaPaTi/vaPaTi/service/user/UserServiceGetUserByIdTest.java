package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.dtos.UserDTO;
import com.vaPaTi.vaPaTi.dtos.UserInfoDTO;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.UserMapper;
import com.vaPaTi.vaPaTi.repository.RoleRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - getUserById() Tests")
class UserServiceGetUserByIdTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private UserService userService;

    private Long validUserId;
    private User existingUser;
    private UserDTO expectedUserDTO;
    private Role userRole;
    private UserInfo userInfo;
    private UserInfoDTO userInfoDTO;
    @BeforeEach
    void setUp() {
        validUserId = 1L;

        userRole = Role.builder()
                .id(1L)
                .name("USER")
                .build();

        userInfo = UserInfo.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        userInfoDTO = UserInfoDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        existingUser = User.builder()
                .id(validUserId)
                .active(true)
                .verified(true)
                .deletedAt(null)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .role(userRole)
                .userInfo(userInfo)
                .userCategories(new HashSet<>())
                .bankAccounts(new HashSet<>())
                .build();

        expectedUserDTO = UserDTO.builder()
                .id(validUserId)
                .active(true)
                .verified(true)
                .categories(new ArrayList<>())
                .userInfo(userInfoDTO)
                .bankAccounts(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should successfully return UserDTO when user exists with valid positive ID")
    void getUserById_WhenValidIdAndUserExists_ShouldReturnUserDTO() {
        // Given
        when(userRepository.findByIdWithFullDetails(validUserId)).thenReturn(Optional.of(existingUser));
        when(userMapper.toUserDTO(existingUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.getUserById(validUserId);

        // Then
        assertNotNull(result);
        assertEquals(expectedUserDTO, result);
        assertEquals(validUserId, result.getId());
        assertTrue(result.getActive());
        assertTrue(result.getVerified());
        assertNotNull(result.getUserInfo());
        assertEquals("John", result.getUserInfo().getFirstName());
        assertEquals("Doe", result.getUserInfo().getLastName());
        assertEquals("john.doe@example.com", result.getUserInfo().getEmail());
        assertNotNull(result.getCategories());
        assertNotNull(result.getBankAccounts());

        InOrder inOrder = inOrder(userRepository, userMapper);
        inOrder.verify(userRepository).findByIdWithFullDetails(validUserId);
        inOrder.verify(userMapper).toUserDTO(existingUser);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("Should throw MessageException when user with valid ID does not exist")
    void getUserById_WhenValidIdButUserNotFound_ShouldThrowMessageException() {
        // Given
        Long nonExistentUserId = 999L;
        when(userRepository.findByIdWithFullDetails(nonExistentUserId)).thenReturn(Optional.empty());

        // When & Then
        MessageException exception = assertThrows(MessageException.class, () -> {
            userService.getUserById(nonExistentUserId);
        });

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findByIdWithFullDetails(nonExistentUserId);
        verify(userMapper, never()).toUserDTO(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when ID is null")
    void getUserById_WhenIdIsNull_ShouldThrowIllegalArgumentException() {
        // Given
        Long nullId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(nullId);
        });

        assertEquals("ID must be a positive number", exception.getMessage());

        verify(userRepository, never()).findByIdWithFullDetails(any());
        verify(userMapper, never()).toUserDTO(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when ID is zero")
    void getUserById_WhenIdIsZero_ShouldThrowIllegalArgumentException() {
        // Given
        Long zeroId = 0L;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(zeroId);
        });

        assertEquals("ID must be a positive number", exception.getMessage());

        verify(userRepository, never()).findByIdWithFullDetails(any());
        verify(userMapper, never()).toUserDTO(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when ID is negative")
    void getUserById_WhenIdIsNegative_ShouldThrowIllegalArgumentException() {
        // Given
        Long negativeId = -1L;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(negativeId);
        });

        assertEquals("ID must be a positive number", exception.getMessage());

        verify(userRepository, never()).findByIdWithFullDetails(any());
        verify(userMapper, never()).toUserDTO(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when ID is large negative number")
    void getUserById_WhenIdIsLargeNegative_ShouldThrowIllegalArgumentException() {
        // Given
        Long largeNegativeId = -999999L;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(largeNegativeId);
        });

        assertEquals("ID must be a positive number", exception.getMessage());

        verify(userRepository, never()).findByIdWithFullDetails(any());
        verify(userMapper, never()).toUserDTO(any(User.class));
    }

    @Test
    @DisplayName("Should handle edge case with minimum valid positive ID")
    void getUserById_WhenIdIsOne_ShouldProcessSuccessfully() {
        // Given
        Long minValidId = 1L;
        User userWithMinId = User.builder()
                .id(minValidId)
                .active(true)
                .verified(false)
                .role(userRole)
                .userInfo(userInfo)
                .build();

        UserDTO expectedDTO = UserDTO.builder()
                .id(minValidId)
                .active(true)
                .verified(false)
                .categories(new ArrayList<>())
                .bankAccounts(new ArrayList<>())
                .build();

        when(userRepository.findByIdWithFullDetails(minValidId)).thenReturn(Optional.of(userWithMinId));
        when(userMapper.toUserDTO(userWithMinId)).thenReturn(expectedDTO);

        // When
        UserDTO result = userService.getUserById(minValidId);

        // Then
        assertNotNull(result);
        assertEquals(minValidId, result.getId());

        verify(userRepository).findByIdWithFullDetails(minValidId);
        verify(userMapper).toUserDTO(userWithMinId);
    }

    @Test
    @DisplayName("Should handle edge case with maximum possible Long ID")
    void getUserById_WhenIdIsMaxLong_ShouldProcessSuccessfully() {
        // Given
        Long maxId = Long.MAX_VALUE;
        when(userRepository.findByIdWithFullDetails(maxId)).thenReturn(Optional.empty());

        // When & Then
        MessageException exception = assertThrows(MessageException.class, () -> {
            userService.getUserById(maxId);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByIdWithFullDetails(maxId);
        verify(userMapper, never()).toUserDTO(any(User.class));
    }

    @Test
    @DisplayName("Should verify exact interaction sequence for successful retrieval")
    void getUserById_SuccessfulRetrieval_ShouldFollowExactInteractionSequence() {
        // Given
        when(userRepository.findByIdWithFullDetails(validUserId)).thenReturn(Optional.of(existingUser));
        when(userMapper.toUserDTO(existingUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.getUserById(validUserId);

        // Then
        InOrder inOrder = inOrder(userRepository, userMapper);
        inOrder.verify(userRepository, times(1)).findByIdWithFullDetails(validUserId);
        inOrder.verify(userMapper, times(1)).toUserDTO(existingUser);
        inOrder.verifyNoMoreInteractions();

        assertSame(expectedUserDTO, result);
    }

    @Test
    @DisplayName("Should call findByIdWithFullDetails with exact parameter and return mapper result")
    void getUserById_WhenSuccessful_ShouldUseCorrectRepositoryMethodAndMapper() {
        // Given
        when(userRepository.findByIdWithFullDetails(validUserId)).thenReturn(Optional.of(existingUser));
        when(userMapper.toUserDTO(existingUser)).thenReturn(expectedUserDTO);

        // When
        UserDTO result = userService.getUserById(validUserId);

        // Then
        verify(userRepository).findByIdWithFullDetails(validUserId);
        verify(userRepository, never()).findById(any()); // Ensure specific method is used
        verify(userMapper).toUserDTO(existingUser);
        verify(userMapper).toUserDTO(argThat(user ->
                user.getId().equals(validUserId) &&
                        user.isActive() &&
                        user.isVerified()
        ));

        assertSame(expectedUserDTO, result);
    }

    @Test
    @DisplayName("Should handle user with minimal data successfully")
    void getUserById_WhenUserHasMinimalData_ShouldReturnDTO() {
        // Given
        User minimalUser = User.builder()
                .id(validUserId)
                .active(false)
                .verified(false)
                .deletedAt(null)
                .build();

        UserDTO minimalDTO = UserDTO.builder()
                .id(validUserId)
                .active(false)
                .verified(false)
                .categories(new ArrayList<>())
                .bankAccounts(new ArrayList<>())
                .build();

        when(userRepository.findByIdWithFullDetails(validUserId)).thenReturn(Optional.of(minimalUser));
        when(userMapper.toUserDTO(minimalUser)).thenReturn(minimalDTO);

        // When
        UserDTO result = userService.getUserById(validUserId);

        // Then
        assertNotNull(result);
        assertEquals(validUserId, result.getId());
        assertFalse(result.getActive());
        assertFalse(result.getVerified());

        verify(userRepository).findByIdWithFullDetails(validUserId);
        verify(userMapper).toUserDTO(minimalUser);
    }

    @Test
    @DisplayName("Should not catch or handle mapper exceptions - let them propagate")
    void getUserById_WhenMapperThrowsException_ShouldPropagateException() {
        // Given
        RuntimeException mapperException = new RuntimeException("Mapping failed");
        when(userRepository.findByIdWithFullDetails(validUserId)).thenReturn(Optional.of(existingUser));
        when(userMapper.toUserDTO(existingUser)).thenThrow(mapperException);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(validUserId);
        });

        assertEquals("Mapping failed", exception.getMessage());
        assertSame(mapperException, exception);

        verify(userRepository).findByIdWithFullDetails(validUserId);
        verify(userMapper).toUserDTO(existingUser);
    }

}
