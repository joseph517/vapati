package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.UserDTO;
import com.vaPaTi.vaPaTi.dtos.UserInfoDTO;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.mapper.UserMapper;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - listUsers() Tests")
class UserServiceListUsersTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;
    private UserDTO userDTO1;
    private UserDTO userDTO2;
    private List<User> users;
    private List<UserDTO> expectedUserDTOs;

    @BeforeEach
    void setUp() {
        // Setup User entities
        user1 = User.builder()
                .id(1L)
                .active(true)
                .verified(true)
                .userInfo(UserInfo.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .build())
                .bankAccounts(Set.of(
                        BankAccount.builder()
                                .id(1L)
                                .accountNumber("123456789")
                                .build()
                ))
                .build();

        user2 = User.builder()
                .id(2L)
                .active(false)
                .verified(false)
                .userInfo(UserInfo.builder()
                        .firstName("Jane")
                        .lastName("Smith")
                        .build())
                .bankAccounts(new HashSet<>())
                .build();

        // Setup UserDTO objects
        userDTO1 = new UserDTO();
        userDTO1.setId(1L);
        userDTO1.setActive(true);
        userDTO1.setVerified(true);
        userDTO1.setCategories(List.of("PREMIUM", "VERIFIED"));
        userDTO1.setUserInfo(UserInfoDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .build());
        userDTO1.setBankAccounts(List.of(
                BankAccountDTO.builder()
                        .id(1L)
                        .accountNumber("123456789")
                        .build()
        ));

        userDTO2 = new UserDTO();
        userDTO2.setId(2L);
        userDTO2.setActive(false);
        userDTO2.setVerified(false);
        userDTO2.setCategories(new ArrayList<>());
        userDTO2.setUserInfo(UserInfoDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build());
        userDTO2.setBankAccounts(new ArrayList<>());

        users = List.of(user1, user2);
        expectedUserDTOs = List.of(userDTO1, userDTO2);
    }

    @Test
    @DisplayName("Should return list of UserDTOs when users exist")
    void listUsers_WhenUsersExist_ShouldReturnUserDTOList() {
        // Given
        when(userRepository.findAllWithDetails()).thenReturn(users);
        when(userMapper.toUserDTO(user1)).thenReturn(userDTO1);
        when(userMapper.toUserDTO(user2)).thenReturn(userDTO2);

        // When
        List<UserDTO> result = userService.listUsers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(userDTO1, userDTO2);

        // Verify interactions
        verify(userRepository).findAllWithDetails();
        verify(userMapper).toUserDTO(user1);
        verify(userMapper).toUserDTO(user2);

        // Verify order of execution
        InOrder inOrder = inOrder(userRepository, userMapper);
        inOrder.verify(userRepository).findAllWithDetails();
        inOrder.verify(userMapper).toUserDTO(user1);
        inOrder.verify(userMapper).toUserDTO(user2);

        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void listUsers_WhenNoUsersExist_ShouldReturnEmptyList() {
        // Given
        when(userRepository.findAllWithDetails()).thenReturn(Collections.emptyList());

        // When
        List<UserDTO> result = userService.listUsers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // Verify interactions
        verify(userRepository).findAllWithDetails();
        verifyNoInteractions(userMapper);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Should return single UserDTO when only one user exists")
    void listUsers_WhenSingleUserExists_ShouldReturnSingleElementList() {
        // Given
        List<User> singleUserList = List.of(user1);
        when(userRepository.findAllWithDetails()).thenReturn(singleUserList);
        when(userMapper.toUserDTO(user1)).thenReturn(userDTO1);

        // When
        List<UserDTO> result = userService.listUsers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(userDTO1);
        assertThat(result.get(0)).isEqualTo(userDTO1);

        // Verify interactions
        verify(userRepository).findAllWithDetails();
        verify(userMapper).toUserDTO(user1);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Should handle mapper returning null for some users")
    void listUsers_WhenMapperReturnsNullForSomeUsers_ShouldIncludeNullsInResult() {
        // Given
        when(userRepository.findAllWithDetails()).thenReturn(users);
        when(userMapper.toUserDTO(user1)).thenReturn(userDTO1);
        when(userMapper.toUserDTO(user2)).thenReturn(null); // Mapper returns null

        // When
        List<UserDTO> result = userService.listUsers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(userDTO1, null);

        // Verify interactions
        verify(userRepository).findAllWithDetails();
        verify(userMapper).toUserDTO(user1);
        verify(userMapper).toUserDTO(user2);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Should handle large list of users efficiently")
    void listUsers_WhenLargeNumberOfUsers_ShouldHandleEfficiently() {
        // Given
        List<User> largeUserList = new ArrayList<>();
        List<UserDTO> expectedLargeDTOList = new ArrayList<>();

        // Create 100 users for performance testing
        for (int i = 1; i <= 100; i++) {
            User user = User.builder()
                    .id((long) i)
                    .active(i % 2 == 0)
                    .verified(i % 3 == 0)
                    .build();

            UserDTO dto = new UserDTO();
            dto.setId((long) i);
            dto.setActive(i % 2 == 0);
            dto.setVerified(i % 3 == 0);

            largeUserList.add(user);
            expectedLargeDTOList.add(dto);

            when(userMapper.toUserDTO(user)).thenReturn(dto);
        }

        when(userRepository.findAllWithDetails()).thenReturn(largeUserList);

        // When
        List<UserDTO> result = userService.listUsers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(100);
        assertThat(result).containsExactlyElementsOf(expectedLargeDTOList);

        // Verify repository was called once
        verify(userRepository, times(1)).findAllWithDetails();

        // Verify mapper was called for each user
        verify(userMapper, times(100)).toUserDTO(any(User.class));
    }

    @Test
    @DisplayName("Should maintain order of users from repository")
    void listUsers_ShouldMaintainOrderFromRepository() {
        // Given - reverse order to test ordering is preserved
        List<User> orderedUsers = List.of(user2, user1); // Note: user2 first
        List<UserDTO> expectedOrderedDTOs = List.of(userDTO2, userDTO1);

        when(userRepository.findAllWithDetails()).thenReturn(orderedUsers);
        when(userMapper.toUserDTO(user2)).thenReturn(userDTO2);
        when(userMapper.toUserDTO(user1)).thenReturn(userDTO1);

        // When
        List<UserDTO> result = userService.listUsers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(userDTO2, userDTO1); // Order preserved

        // Verify execution order matches input order
        InOrder inOrder = inOrder(userMapper);
        inOrder.verify(userMapper).toUserDTO(user2); // First call
        inOrder.verify(userMapper).toUserDTO(user1); // Second call
    }

    @Test
    @DisplayName("Should return immutable list")
    void listUsers_ShouldReturnImmutableList() {
        // Given
        when(userRepository.findAllWithDetails()).thenReturn(users);
        when(userMapper.toUserDTO(user1)).thenReturn(userDTO1);
        when(userMapper.toUserDTO(user2)).thenReturn(userDTO2);

        // When
        List<UserDTO> result = userService.listUsers();

        // Then
        assertThat(result).isNotNull();

        // The toList() method returns an immutable list in Java 21
        // Verify it behaves as expected for our use case
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(userDTO1);
        assertThat(result.get(1)).isEqualTo(userDTO2);
    }

    @Test
    @DisplayName("Should call repository method exactly once")
    void listUsers_ShouldCallRepositoryOnce() {
        // Given
        when(userRepository.findAllWithDetails()).thenReturn(users);
        when(userMapper.toUserDTO(any(User.class))).thenReturn(new UserDTO());

        // When
        userService.listUsers();

        // Then
        verify(userRepository, times(1)).findAllWithDetails();
        verify(userRepository, never()).findAll(); // Should not call generic findAll
        verify(userRepository, never()).findById(anyLong()); // Should not call findById
        verifyNoMoreInteractions(userRepository);
    }

}
