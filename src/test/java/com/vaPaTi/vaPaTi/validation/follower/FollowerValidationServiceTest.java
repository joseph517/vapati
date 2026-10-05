package com.vaPaTi.vaPaTi.validation.follower;

import com.vaPaTi.vaPaTi.entity.follower.Follower;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.follower.FollowerRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowerValidationService tests")
class FollowerValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowerRepository followerRepository;

    @Mock
    private AccountStatusValidationService accountStatusValidationService;

    @InjectMocks
    private FollowerValidationService followerValidationService;

    private User currentUser;
    private User otherUser;
    private Long currentUserId;
    private Long otherUserId;
    private Long nonExistentUserId;

    @BeforeEach
    void setUp() {
        // Setup test data
        currentUserId = 1L;
        otherUserId = 2L;
        nonExistentUserId = 999L;

        currentUser = User.builder()
                .id(currentUserId)
                .build();

        otherUser = User.builder()
                .id(otherUserId)
                .build();
    }

    @Test
    @DisplayName("Should return false when user tries to follow themselves")
    void isFollowing_WhenCurrentUserIdEqualsOtherUserId_ShouldReturnFalse() {
        // Given
        Long sameUserId = 1L;

        // When
        boolean result = followerValidationService.isFollowing(sameUserId, sameUserId, false);

        // Then
        assertThat(result).isFalse();

        // Verify no repository interactions when users are the same
        verifyNoInteractions(userRepository);
        verifyNoInteractions(followerRepository);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when current user does not exist")
    void isFollowing_WhenCurrentUserNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        when(userRepository.findById(nonExistentUserId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> followerValidationService.isFollowing(nonExistentUserId, otherUserId, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Current user not found with ID: " + nonExistentUserId);

        // Verify interactions
        verify(userRepository).findById(nonExistentUserId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(followerRepository);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when other user does not exist")
    void isFollowing_WhenOtherUserNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        when(userRepository.findById(currentUserId))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.findById(nonExistentUserId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> followerValidationService.isFollowing(currentUserId, nonExistentUserId, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Other user not found with ID: " + nonExistentUserId);

        // Verify interactions with inOrder to ensure proper execution sequence
        var inOrder = inOrder(userRepository);
        inOrder.verify(userRepository).findById(currentUserId);
        inOrder.verify(userRepository).findById(nonExistentUserId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(followerRepository);
    }

    @Test
    @DisplayName("Should return true when current user follows other user")
    void isFollowing_WhenCurrentUserFollowsOtherUser_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(currentUserId))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.findById(otherUserId))
                .thenReturn(Optional.of(otherUser));
        when(followerRepository.existsByUserAndFollower(otherUser, currentUser))
                .thenReturn(true);

        // When
        boolean result = followerValidationService.isFollowing(currentUserId, otherUserId, false);

        // Then
        assertThat(result).isTrue();

        // Verify all interactions with proper order
        var inOrder = inOrder(userRepository, followerRepository);
        inOrder.verify(userRepository).findById(currentUserId);
        inOrder.verify(userRepository).findById(otherUserId);
        inOrder.verify(followerRepository).existsByUserAndFollower(otherUser, currentUser);
        verifyNoMoreInteractions(userRepository, followerRepository);
    }

    @Test
    @DisplayName("Should return false when current user does not follow other user")
    void isFollowing_WhenCurrentUserDoesNotFollowOtherUser_ShouldReturnFalse() {
        // Given
        when(userRepository.findById(currentUserId))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.findById(otherUserId))
                .thenReturn(Optional.of(otherUser));
        when(followerRepository.existsByUserAndFollower(otherUser, currentUser))
                .thenReturn(false);

        // When
        boolean result = followerValidationService.isFollowing(currentUserId, otherUserId, false);

        // Then
        assertThat(result).isFalse();

        // Verify all interactions with proper order
        var inOrder = inOrder(userRepository, followerRepository);
        inOrder.verify(userRepository).findById(currentUserId);
        inOrder.verify(userRepository).findById(otherUserId);
        inOrder.verify(followerRepository).existsByUserAndFollower(otherUser, currentUser);
        verifyNoMoreInteractions(userRepository, followerRepository);
    }

    @Test
    @DisplayName("Should handle null otherUserId parameter correctly")
    void isFollowing_WhenOtherUserIdIsNull_ShouldThrowResourceNotFoundException() {
        // Given
        when(userRepository.findById(currentUserId))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.findById(eq(null)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> followerValidationService.isFollowing(currentUserId, null, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Other user not found with ID: null");

        // Verify interactions
        var inOrder = inOrder(userRepository);
        inOrder.verify(userRepository).findById(currentUserId);
        inOrder.verify(userRepository).findById(eq(null));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(followerRepository);
    }

    @Test
    @DisplayName("Should verify correct parameters are passed to followerRepository.existsByUserAndFollower")
    void isFollowing_ShouldPassCorrectParametersToFollowerRepository() {
        // Given
        when(userRepository.findById(currentUserId))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.findById(otherUserId))
                .thenReturn(Optional.of(otherUser));
        when(followerRepository.existsByUserAndFollower(any(User.class), any(User.class)))
                .thenReturn(true);

        // When
        followerValidationService.isFollowing(currentUserId, otherUserId, false);

        // Then - Verify the correct order of parameters: (otherUser, currentUser)
        // This tests that we're checking if currentUser follows otherUser, not the other way around
        verify(followerRepository).existsByUserAndFollower(
                eq(otherUser),  // The user being followed
                eq(currentUser) // The follower
        );
    }

    @Test
    @DisplayName("Should handle edge case with same user IDs but different objects")
    void isFollowing_WhenSameUserIdButDifferentCurrentUserIdObject_ShouldReturnFalse() {
        // Given - Testing with different Long objects that have same value
        Long currentUserIdObj = Long.valueOf(1);
        Long otherUserIdObj = Long.valueOf(1);

        // When
        boolean result = followerValidationService.isFollowing(currentUserIdObj, otherUserIdObj, false);

        // Then
        assertThat(result).isFalse();

        // Verify no repository interactions
        verifyNoInteractions(userRepository);
        verifyNoInteractions(followerRepository);
    }

    @Test
    @DisplayName("Should handle repository exceptions correctly")
    void isFollowing_WhenUserRepositoryThrowsException_ShouldPropagateException() {
        // Given
        RuntimeException expectedException = new RuntimeException("Database connection error");
        when(userRepository.findById(currentUserId))
                .thenThrow(expectedException);

        // When & Then
        assertThatThrownBy(() -> followerValidationService.isFollowing(currentUserId, otherUserId, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection error");

        // Verify only first repository call was made
        verify(userRepository).findById(currentUserId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(followerRepository);
    }

    @Test
    @DisplayName("Should handle follower repository exceptions correctly")
    void isFollowing_WhenFollowerRepositoryThrowsException_ShouldPropagateException() {
        // Given
        RuntimeException expectedException = new RuntimeException("Database query error");
        when(userRepository.findById(currentUserId))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.findById(otherUserId))
                .thenReturn(Optional.of(otherUser));
        when(followerRepository.existsByUserAndFollower(otherUser, currentUser))
                .thenThrow(expectedException);

        // When & Then
        assertThatThrownBy(() -> followerValidationService.isFollowing(currentUserId, otherUserId, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database query error");

        // Verify all expected interactions occurred
        var inOrder = inOrder(userRepository, followerRepository);
        inOrder.verify(userRepository).findById(currentUserId);
        inOrder.verify(userRepository).findById(otherUserId);
        inOrder.verify(followerRepository).existsByUserAndFollower(otherUser, currentUser);
        verifyNoMoreInteractions(userRepository, followerRepository);
    }

    @Nested
    @DisplayName("validateNotSelfFollow() / validateNotSelfUnfollow()")
    class SelfActionTests {

        @Test
        @DisplayName("validateNotSelfFollow should not throw when the IDs differ")
        void validateNotSelfFollow_WithDifferentIds_ShouldNotThrow() {
            assertThatCode(() -> followerValidationService.validateNotSelfFollow(currentUserId, otherUserId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateNotSelfFollow should throw MessageException when the IDs are equal")
        void validateNotSelfFollow_WithSameIds_ShouldThrow() {
            assertThatThrownBy(() -> followerValidationService.validateNotSelfFollow(currentUserId, Long.valueOf(1)))
                    .isExactlyInstanceOf(MessageException.class)
                    .hasMessage("Users cannot follow themselves");
        }

        @Test
        @DisplayName("validateNotSelfUnfollow should not throw when the IDs differ")
        void validateNotSelfUnfollow_WithDifferentIds_ShouldNotThrow() {
            assertThatCode(() -> followerValidationService.validateNotSelfUnfollow(currentUserId, otherUserId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateNotSelfUnfollow should throw MessageException when the IDs are equal")
        void validateNotSelfUnfollow_WithSameIds_ShouldThrow() {
            assertThatThrownBy(() -> followerValidationService.validateNotSelfUnfollow(currentUserId, Long.valueOf(1)))
                    .isExactlyInstanceOf(MessageException.class)
                    .hasMessage("Users cannot unfollow themselves");
        }
    }

    @Nested
    @DisplayName("validateAndGet*User()")
    class ValidateAndGetUserTests {

        @Test
        @DisplayName("validateAndGetCurrentUser should return the user when it exists")
        void validateAndGetCurrentUser_WhenExists_ShouldReturnUser() {
            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

            assertThat(followerValidationService.validateAndGetCurrentUser(currentUserId)).isSameAs(currentUser);
        }

        @Test
        @DisplayName("validateAndGetCurrentUser should throw ResourceNotFoundException when it does not exist")
        void validateAndGetCurrentUser_WhenNotFound_ShouldThrow() {
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidationService.validateAndGetCurrentUser(nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Current user not found with ID: " + nonExistentUserId);
        }

        @Test
        @DisplayName("validateAndGetUserToFollow should return the user when it exists")
        void validateAndGetUserToFollow_WhenExists_ShouldReturnUser() {
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

            assertThat(followerValidationService.validateAndGetUserToFollow(otherUserId)).isSameAs(otherUser);
        }

        @Test
        @DisplayName("validateAndGetUserToFollow should throw ResourceNotFoundException when it does not exist")
        void validateAndGetUserToFollow_WhenNotFound_ShouldThrow() {
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidationService.validateAndGetUserToFollow(nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to follow not found with ID: " + nonExistentUserId);
        }

        @Test
        @DisplayName("validateAndGetUserToUnfollow should return the user when it exists")
        void validateAndGetUserToUnfollow_WhenExists_ShouldReturnUser() {
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

            assertThat(followerValidationService.validateAndGetUserToUnfollow(otherUserId)).isSameAs(otherUser);
        }

        @Test
        @DisplayName("validateAndGetUserToUnfollow should throw ResourceNotFoundException when it does not exist")
        void validateAndGetUserToUnfollow_WhenNotFound_ShouldThrow() {
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidationService.validateAndGetUserToUnfollow(nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to unfollow not found with ID: " + nonExistentUserId);
        }

        @Test
        @DisplayName("validateAndGetVisibleUser should return the user when it exists")
        void validateAndGetVisibleUser_WhenExists_ShouldReturnUser() {
            when(userRepository.findByIdForRequest(otherUserId)).thenReturn(Optional.of(otherUser));

            assertThat(followerValidationService.validateAndGetVisibleUser(otherUserId, false)).isSameAs(otherUser);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("validateAndGetVisibleUser should throw ResourceNotFoundException when it does not exist")
        void validateAndGetVisibleUser_WhenNotFound_ShouldThrow() {
            when(userRepository.findByIdForRequest(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidationService.validateAndGetVisibleUser(nonExistentUserId, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with ID: " + nonExistentUserId);
            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Blocked users (spec 45)")
    class BlockedUserTests {

        @Test
        @DisplayName("validateAndGetVisibleUser with a blocked user and a non-admin: 404")
        void validateAndGetVisibleUser_WhenBlockedAndNotAdmin_ShouldThrow() {
            when(userRepository.findByIdForRequest(otherUserId)).thenReturn(Optional.of(otherUser));
            when(accountStatusValidationService.isBlocked(otherUser)).thenReturn(true);

            assertThatThrownBy(() -> followerValidationService.validateAndGetVisibleUser(otherUserId, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with ID: " + otherUserId);
        }

        @Test
        @DisplayName("validateAndGetVisibleUser with a blocked user and an ADMIN: returns the user")
        void validateAndGetVisibleUser_WhenBlockedAndAdmin_ShouldReturnUser() {
            when(userRepository.findByIdForRequest(otherUserId)).thenReturn(Optional.of(otherUser));

            assertThat(followerValidationService.validateAndGetVisibleUser(otherUserId, true)).isSameAs(otherUser);
            verify(accountStatusValidationService, never()).isBlocked(any());
        }

        @Test
        @DisplayName("validateAndGetUserToFollow with a blocked user: 404, for anyone")
        void validateAndGetUserToFollow_WhenBlocked_ShouldThrow() {
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
            when(accountStatusValidationService.isBlocked(otherUser)).thenReturn(true);

            assertThatThrownBy(() -> followerValidationService.validateAndGetUserToFollow(otherUserId))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to follow not found with ID: " + otherUserId);
        }

        @Test
        @DisplayName("validateAndGetUserToUnfollow with a blocked user: 404, for anyone")
        void validateAndGetUserToUnfollow_WhenBlocked_ShouldThrow() {
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
            when(accountStatusValidationService.isBlocked(otherUser)).thenReturn(true);

            assertThatThrownBy(() -> followerValidationService.validateAndGetUserToUnfollow(otherUserId))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to unfollow not found with ID: " + otherUserId);
        }

        @Test
        @DisplayName("isFollowing with the current user blocked and a non-admin: 404")
        void isFollowing_WhenCurrentUserBlockedAndNotAdmin_ShouldThrow() {
            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
            when(accountStatusValidationService.isBlocked(currentUser)).thenReturn(true);

            assertThatThrownBy(() -> followerValidationService.isFollowing(currentUserId, otherUserId, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Current user not found with ID: " + currentUserId);
            verifyNoInteractions(followerRepository);
        }

        @Test
        @DisplayName("isFollowing with the other user blocked and a non-admin: 404")
        void isFollowing_WhenOtherUserBlockedAndNotAdmin_ShouldThrow() {
            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
            when(accountStatusValidationService.isBlocked(currentUser)).thenReturn(false);
            when(accountStatusValidationService.isBlocked(otherUser)).thenReturn(true);

            assertThatThrownBy(() -> followerValidationService.isFollowing(currentUserId, otherUserId, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Other user not found with ID: " + otherUserId);
            verifyNoInteractions(followerRepository);
        }

        @Test
        @DisplayName("isFollowing with a blocked user and an ADMIN: returns the boolean")
        void isFollowing_WhenBlockedAndAdmin_ShouldReturnResult() {
            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
            when(followerRepository.existsByUserAndFollower(otherUser, currentUser)).thenReturn(true);

            assertThat(followerValidationService.isFollowing(currentUserId, otherUserId, true)).isTrue();
            verify(accountStatusValidationService, never()).isBlocked(any());
        }

        @Test
        @DisplayName("isAdmin reads the role from the database")
        void isAdmin_ShouldQueryRole() {
            when(userRepository.existsByIdAndRole_Name(currentUserId, "ADMIN")).thenReturn(true);

            assertThat(followerValidationService.isAdmin(currentUserId)).isTrue();
        }

        @Test
        @DisplayName("isAdmin with a null id: false without querying")
        void isAdmin_WithNullId_ShouldReturnFalse() {
            assertThat(followerValidationService.isAdmin(null)).isFalse();
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("validateNotAlreadyFollowing() / validateAndGetFollowRelation()")
    class RelationTests {

        @Test
        @DisplayName("validateNotAlreadyFollowing should not throw when the relationship does not exist")
        void validateNotAlreadyFollowing_WhenNotFollowing_ShouldNotThrow() {
            when(followerRepository.existsByUserAndFollower(otherUser, currentUser)).thenReturn(false);

            assertThatCode(() -> followerValidationService.validateNotAlreadyFollowing(otherUser, currentUser))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateNotAlreadyFollowing should throw ConflictException when the relationship exists")
        void validateNotAlreadyFollowing_WhenAlreadyFollowing_ShouldThrow() {
            when(followerRepository.existsByUserAndFollower(otherUser, currentUser)).thenReturn(true);

            assertThatThrownBy(() -> followerValidationService.validateNotAlreadyFollowing(otherUser, currentUser))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("User is already being followed");
        }

        @Test
        @DisplayName("validateAndGetFollowRelation should return the relationship when it exists")
        void validateAndGetFollowRelation_WhenExists_ShouldReturnRelation() {
            Follower relation = new Follower();
            when(followerRepository.findByUserAndFollower(otherUser, currentUser)).thenReturn(Optional.of(relation));

            assertThat(followerValidationService.validateAndGetFollowRelation(otherUser, currentUser)).isSameAs(relation);
        }

        @Test
        @DisplayName("validateAndGetFollowRelation should throw ResourceNotFoundException when it does not exist")
        void validateAndGetFollowRelation_WhenNotFound_ShouldThrow() {
            when(followerRepository.findByUserAndFollower(otherUser, currentUser)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidationService.validateAndGetFollowRelation(otherUser, currentUser))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Follow relationship not found");
        }
    }
}
