package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;

import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserValidationService - User Retrieval Operations")
class UserValidationServiceUserRetrievalTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserValidationService userValidationService;

    // Test data constants
    private static final Long VALID_USER_ID = 1L;
    private static final Long NON_EXISTENT_USER_ID = 999L;
    private static final Long ZERO_ID = 0L;
    private static final Long NEGATIVE_ID = -1L;
    private static final String EXPECTED_ERROR_MESSAGE = "User not found";

    private User mockUser;
    private UserInfo mockUserInfo;
    private Role mockRole;

    @BeforeEach
    void setUp() {
        // Setup UserInfo
        mockUserInfo = UserInfo.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .userName("johndoe")
                .password("hashedPassword123")
                .phone("+1234567890")
                .description("Test user description")
                .profilePicture("profile.jpg")
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        // Setup Role
        mockRole = Role.builder()
                .id(1L)
                .name("USER")
                .build();

        // Setup User with all relationships
        mockUser = User.builder()
                .id(VALID_USER_ID)
                .active(true)
                .verified(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .userInfo(mockUserInfo)
                .role(mockRole)
                .userCategories(new HashSet<>())
                .bankAccounts(new HashSet<>())
                .campaigns(new ArrayList<>())
                .build();

        // Set bidirectional relationship
        mockUserInfo.setUser(mockUser);
    }

    @Nested
    @DisplayName("getUserById() method tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when valid ID is provided and user exists")
        void shouldReturnUser_WhenValidIdProvidedAndUserExists() {
            // Given
            given(userRepository.findById(VALID_USER_ID)).willReturn(Optional.of(mockUser));

            // When
            User result = userValidationService.getUserById(VALID_USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VALID_USER_ID);
            assertThat(result.isActive()).isTrue();
            assertThat(result.isVerified()).isTrue();
            assertThat(result.getDeletedAt()).isNull();

            // Verify UserInfo relationship
            assertThat(result.getUserInfo()).isNotNull();
            assertThat(result.getUserInfo().getFirstName()).isEqualTo("John");
            assertThat(result.getUserInfo().getLastName()).isEqualTo("Doe");
            assertThat(result.getUserInfo().getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getUserInfo().getUserName()).isEqualTo("johndoe");

            // Verify Role relationship
            assertThat(result.getRole()).isNotNull();
            assertThat(result.getRole().getName()).isEqualTo("USER");

            // Verify collections are initialized
            assertThat(result.getUserCategories()).isNotNull().isEmpty();
            assertThat(result.getBankAccounts()).isNotNull().isEmpty();
            assertThat(result.getCampaigns()).isNotNull().isEmpty();

            // Verify repository interaction
            verify(userRepository, times(1)).findById(VALID_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should throw MessageException when user does not exist")
        void shouldThrowMessageException_WhenUserDoesNotExist() {
            // Given
            given(userRepository.findById(NON_EXISTENT_USER_ID)).willReturn(Optional.empty());

            // When & Then
            MessageException exception = assertThrows(MessageException.class, () ->
                    userValidationService.getUserById(NON_EXISTENT_USER_ID));
            assertThat(exception.getMessage()).isEqualTo(EXPECTED_ERROR_MESSAGE);

            // Verify repository interaction
            verify(userRepository, times(1)).findById(NON_EXISTENT_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should throw MessageException when ID is zero")
        void shouldThrowMessageException_WhenIdIsZero() {
            // Given
            given(userRepository.findById(ZERO_ID)).willReturn(Optional.empty());

            // When & Then
            MessageException exception = assertThrows(MessageException.class, () ->
                    userValidationService.getUserById(ZERO_ID));
            assertThat(exception.getMessage()).isEqualTo(EXPECTED_ERROR_MESSAGE);

            // Verify repository interaction
            verify(userRepository, times(1)).findById(ZERO_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should throw MessageException when ID is negative")
        void shouldThrowMessageException_WhenIdIsNegative() {
            // Given
            given(userRepository.findById(NEGATIVE_ID)).willReturn(Optional.empty());

            // When & Then
            MessageException exception = assertThrows(MessageException.class, () ->
                    userValidationService.getUserById(NEGATIVE_ID));
            assertThat(exception.getMessage()).isEqualTo(EXPECTED_ERROR_MESSAGE);

            // Verify repository interaction
            verify(userRepository, times(1)).findById(NEGATIVE_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should handle null ID gracefully by delegating to repository")
        void shouldHandleNullId_ByDelegatingToRepository() {
            // Given
            given(userRepository.findById(null)).willReturn(Optional.empty());

            // When & Then
            MessageException exception = assertThrows(MessageException.class, () ->
                    userValidationService.getUserById(null));
            assertThat(exception.getMessage()).isEqualTo(EXPECTED_ERROR_MESSAGE);

            // Verify repository interaction
            verify(userRepository, times(1)).findById(null);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should return user even when user has minimal required data")
        void shouldReturnUser_WhenUserHasMinimalRequiredData() {
            // Given - User with only required fields
            UserInfo minimalUserInfo = UserInfo.builder()
                    .id(1L)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .userName("janesmith")
                    .password("hashedPassword456")
                    .phone("+0987654321")
                    .description("Minimal user")
                    .build();

            Role minimalRole = Role.builder()
                    .id(2L)
                    .name("BASIC_USER")
                    .build();

            User minimalUser = User.builder()
                    .id(VALID_USER_ID)
                    .active(false)  // Can be inactive
                    .verified(false)  // Can be unverified
                    .createdAt(LocalDateTime.now())
                    .userInfo(minimalUserInfo)
                    .role(minimalRole)
                    .userCategories(new HashSet<>())
                    .bankAccounts(new HashSet<>())
                    .campaigns(new ArrayList<>())
                    .build();

            minimalUserInfo.setUser(minimalUser);
            given(userRepository.findById(VALID_USER_ID)).willReturn(Optional.of(minimalUser));

            // When
            User result = userValidationService.getUserById(VALID_USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VALID_USER_ID);
            assertThat(result.isActive()).isFalse();
            assertThat(result.isVerified()).isFalse();
            assertThat(result.getUserInfo()).isNotNull();
            assertThat(result.getUserInfo().getFirstName()).isEqualTo("Jane");
            assertThat(result.getRole().getName()).isEqualTo("BASIC_USER");

            // Verify repository interaction
            verify(userRepository, times(1)).findById(VALID_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should return active user with complete profile data")
        void shouldReturnActiveUser_WithCompleteProfileData() {
            // Given - User with complete profile including optional fields
            UserInfo completeUserInfo = UserInfo.builder()
                    .id(1L)
                    .firstName("Alexander")
                    .lastName("Johnson")
                    .email("alex.johnson@example.com")
                    .userName("alexjohnson")
                    .password("secureHashedPassword789")
                    .phone("+1122334455")
                    .description("Complete user profile with all details")
                    .profilePicture("https://example.com/profiles/alex.jpg")
                    .createdAt(LocalDateTime.now().minusMonths(6))
                    .updatedAt(LocalDateTime.now().minusHours(2))
                    .build();

            User completeUser = User.builder()
                    .id(VALID_USER_ID)
                    .active(true)
                    .verified(true)
                    .createdAt(LocalDateTime.now().minusMonths(6))
                    .updatedAt(LocalDateTime.now().minusHours(2))
                    .userInfo(completeUserInfo)
                    .role(mockRole)
                    .userCategories(new HashSet<>())
                    .bankAccounts(new HashSet<>())
                    .campaigns(new ArrayList<>())
                    .build();

            completeUserInfo.setUser(completeUser);
            given(userRepository.findById(VALID_USER_ID)).willReturn(Optional.of(completeUser));

            // When
            User result = userValidationService.getUserById(VALID_USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VALID_USER_ID);
            assertThat(result.isActive()).isTrue();
            assertThat(result.isVerified()).isTrue();

            // Verify complete UserInfo
            UserInfo resultUserInfo = result.getUserInfo();
            assertThat(resultUserInfo).isNotNull();
            assertThat(resultUserInfo.getFirstName()).isEqualTo("Alexander");
            assertThat(resultUserInfo.getLastName()).isEqualTo("Johnson");
            assertThat(resultUserInfo.getEmail()).isEqualTo("alex.johnson@example.com");
            assertThat(resultUserInfo.getUserName()).isEqualTo("alexjohnson");
            assertThat(resultUserInfo.getPhone()).isEqualTo("+1122334455");
            assertThat(resultUserInfo.getDescription()).isEqualTo("Complete user profile with all details");
            assertThat(resultUserInfo.getProfilePicture()).isEqualTo("https://example.com/profiles/alex.jpg");
            assertThat(resultUserInfo.getCreatedAt()).isNotNull();
            assertThat(resultUserInfo.getUpdatedAt()).isNotNull();

            // Verify repository interaction
            verify(userRepository, times(1)).findById(VALID_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("existsById() method tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when user exists with valid ID")
        void shouldReturnTrue_WhenUserExistsWithValidId() {
            // Given
            given(userRepository.existsById(VALID_USER_ID)).willReturn(true);

            // When
            boolean result = userValidationService.existsById(VALID_USER_ID);

            // Then
            assertThat(result).isTrue();

            // Verify repository interaction
            verify(userRepository, times(1)).existsById(VALID_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should return false when user does not exist")
        void shouldReturnFalse_WhenUserDoesNotExist() {
            // Given
            given(userRepository.existsById(NON_EXISTENT_USER_ID)).willReturn(false);

            // When
            boolean result = userValidationService.existsById(NON_EXISTENT_USER_ID);

            // Then
            assertThat(result).isFalse();

            // Verify repository interaction
            verify(userRepository, times(1)).existsById(NON_EXISTENT_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should return false when ID is zero")
        void shouldReturnFalse_WhenIdIsZero() {
            // Given
            given(userRepository.existsById(ZERO_ID)).willReturn(false);

            // When
            boolean result = userValidationService.existsById(ZERO_ID);

            // Then
            assertThat(result).isFalse();

            // Verify repository interaction
            verify(userRepository, times(1)).existsById(ZERO_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should return false when ID is negative")
        void shouldReturnFalse_WhenIdIsNegative() {
            // Given
            given(userRepository.existsById(NEGATIVE_ID)).willReturn(false);

            // When
            boolean result = userValidationService.existsById(NEGATIVE_ID);

            // Then
            assertThat(result).isFalse();

            // Verify repository interaction
            verify(userRepository, times(1)).existsById(NEGATIVE_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should handle null ID by delegating to repository")
        void shouldHandleNullId_ByDelegatingToRepository() {
            // Given
            given(userRepository.existsById(null)).willReturn(false);

            // When
            boolean result = userValidationService.existsById(null);

            // Then
            assertThat(result).isFalse();

            // Verify repository interaction
            verify(userRepository, times(1)).existsById(null);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should return true even for edge case IDs when repository confirms existence")
        void shouldReturnTrue_ForEdgeCaseIdsWhenRepositoryConfirmsExistence() {
            // Given
            Long maxLongId = Long.MAX_VALUE;
            given(userRepository.existsById(maxLongId)).willReturn(true);

            // When
            boolean result = userValidationService.existsById(maxLongId);

            // Then
            assertThat(result).isTrue();

            // Verify repository interaction
            verify(userRepository, times(1)).existsById(maxLongId);
            verifyNoMoreInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("Integration behavior tests")
    class IntegrationBehaviorTests {

        @Test
        @DisplayName("Should demonstrate consistent behavior between getUserById and existsById for existing user")
        void shouldDemonstrateConsistentBehavior_ForExistingUser() {
            // Given
            given(userRepository.existsById(VALID_USER_ID)).willReturn(true);
            given(userRepository.findById(VALID_USER_ID)).willReturn(Optional.of(mockUser));

            // When
            boolean exists = userValidationService.existsById(VALID_USER_ID);
            User user = userValidationService.getUserById(VALID_USER_ID);

            // Then
            assertThat(exists).isTrue();
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(VALID_USER_ID);

            // Verify execution order and interactions
            InOrder inOrder = inOrder(userRepository);
            inOrder.verify(userRepository).existsById(VALID_USER_ID);
            inOrder.verify(userRepository).findById(VALID_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should demonstrate soft delete behavior - user not found when deleted")
        void shouldDemonstrateConsistentBehavior_ForSoftDeletedUser() {
            // Given - User that has been soft deleted (deletedAt is not null)
            Long deletedUserId = 100L;
            given(userRepository.existsById(deletedUserId)).willReturn(false);  // Soft deleted users don't exist in queries
            given(userRepository.findById(deletedUserId)).willReturn(Optional.empty());  // Soft deleted users are filtered out

            // When
            boolean exists = userValidationService.existsById(deletedUserId);

            // Then
            assertThat(exists).isFalse();

            // And when trying to get the user
            assertThatThrownBy(() -> userValidationService.getUserById(deletedUserId))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(EXPECTED_ERROR_MESSAGE);

            // Verify execution order and interactions
            InOrder inOrder = inOrder(userRepository);
            inOrder.verify(userRepository).existsById(deletedUserId);
            inOrder.verify(userRepository).findById(deletedUserId);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should verify user with complete relationships exists")
        void shouldVerifyUserWithCompleteRelationships_Exists() {
            // Given - User exists with all relationships properly set
            given(userRepository.existsById(VALID_USER_ID)).willReturn(true);
            given(userRepository.findById(VALID_USER_ID)).willReturn(Optional.of(mockUser));

            // When
            boolean exists = userValidationService.existsById(VALID_USER_ID);
            User retrievedUser = userValidationService.getUserById(VALID_USER_ID);

            // Then
            assertThat(exists).isTrue();
            assertThat(retrievedUser).isNotNull();

            // Verify that the user has proper bidirectional relationship
            assertThat(retrievedUser.getUserInfo()).isNotNull();
            assertThat(retrievedUser.getUserInfo().getUser()).isEqualTo(retrievedUser);

            // Verify all collections are properly initialized
            assertThat(retrievedUser.getUserCategories()).isNotNull();
            assertThat(retrievedUser.getBankAccounts()).isNotNull();
            assertThat(retrievedUser.getCampaigns()).isNotNull();

            // Verify execution order and interactions
            InOrder inOrder = inOrder(userRepository);
            inOrder.verify(userRepository).existsById(VALID_USER_ID);
            inOrder.verify(userRepository).findById(VALID_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }
    }

}
