package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.entity.Follower;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.FollowerRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.validation.FollowerValidation;
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
@DisplayName("FollowerValidation tests")
class FollowerValidationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowerRepository followerRepository;

    @InjectMocks
    private FollowerValidation followerValidation;

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
        boolean result = followerValidation.isFollowing(sameUserId, sameUserId);

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
        assertThatThrownBy(() -> followerValidation.isFollowing(nonExistentUserId, otherUserId))
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
        assertThatThrownBy(() -> followerValidation.isFollowing(currentUserId, nonExistentUserId))
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
        boolean result = followerValidation.isFollowing(currentUserId, otherUserId);

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
        boolean result = followerValidation.isFollowing(currentUserId, otherUserId);

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
        assertThatThrownBy(() -> followerValidation.isFollowing(currentUserId, null))
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
        followerValidation.isFollowing(currentUserId, otherUserId);

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
        boolean result = followerValidation.isFollowing(currentUserIdObj, otherUserIdObj);

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
        assertThatThrownBy(() -> followerValidation.isFollowing(currentUserId, otherUserId))
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
        assertThatThrownBy(() -> followerValidation.isFollowing(currentUserId, otherUserId))
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
            assertThatCode(() -> followerValidation.validateNotSelfFollow(currentUserId, otherUserId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateNotSelfFollow should throw MessageException when the IDs are equal")
        void validateNotSelfFollow_WithSameIds_ShouldThrow() {
            assertThatThrownBy(() -> followerValidation.validateNotSelfFollow(currentUserId, Long.valueOf(1)))
                    .isExactlyInstanceOf(MessageException.class)
                    .hasMessage("Users cannot follow themselves");
        }

        @Test
        @DisplayName("validateNotSelfUnfollow should not throw when the IDs differ")
        void validateNotSelfUnfollow_WithDifferentIds_ShouldNotThrow() {
            assertThatCode(() -> followerValidation.validateNotSelfUnfollow(currentUserId, otherUserId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateNotSelfUnfollow should throw MessageException when the IDs are equal")
        void validateNotSelfUnfollow_WithSameIds_ShouldThrow() {
            assertThatThrownBy(() -> followerValidation.validateNotSelfUnfollow(currentUserId, Long.valueOf(1)))
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

            assertThat(followerValidation.validateAndGetCurrentUser(currentUserId)).isSameAs(currentUser);
        }

        @Test
        @DisplayName("validateAndGetCurrentUser should throw ResourceNotFoundException when it does not exist")
        void validateAndGetCurrentUser_WhenNotFound_ShouldThrow() {
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidation.validateAndGetCurrentUser(nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Current user not found with ID: " + nonExistentUserId);
        }

        @Test
        @DisplayName("validateAndGetUserToFollow should return the user when it exists")
        void validateAndGetUserToFollow_WhenExists_ShouldReturnUser() {
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

            assertThat(followerValidation.validateAndGetUserToFollow(otherUserId)).isSameAs(otherUser);
        }

        @Test
        @DisplayName("validateAndGetUserToFollow should throw ResourceNotFoundException when it does not exist")
        void validateAndGetUserToFollow_WhenNotFound_ShouldThrow() {
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidation.validateAndGetUserToFollow(nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to follow not found with ID: " + nonExistentUserId);
        }

        @Test
        @DisplayName("validateAndGetUserToUnfollow should return the user when it exists")
        void validateAndGetUserToUnfollow_WhenExists_ShouldReturnUser() {
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

            assertThat(followerValidation.validateAndGetUserToUnfollow(otherUserId)).isSameAs(otherUser);
        }

        @Test
        @DisplayName("validateAndGetUserToUnfollow should throw ResourceNotFoundException when it does not exist")
        void validateAndGetUserToUnfollow_WhenNotFound_ShouldThrow() {
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidation.validateAndGetUserToUnfollow(nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to unfollow not found with ID: " + nonExistentUserId);
        }

        @Test
        @DisplayName("validateAndGetUser should return the user when it exists")
        void validateAndGetUser_WhenExists_ShouldReturnUser() {
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

            assertThat(followerValidation.validateAndGetUser(otherUserId)).isSameAs(otherUser);
        }

        @Test
        @DisplayName("validateAndGetUser should throw ResourceNotFoundException when it does not exist")
        void validateAndGetUser_WhenNotFound_ShouldThrow() {
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidation.validateAndGetUser(nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with ID: " + nonExistentUserId);
        }
    }

    @Nested
    @DisplayName("validateNotAlreadyFollowing() / validateAndGetFollowRelation()")
    class RelationTests {

        @Test
        @DisplayName("validateNotAlreadyFollowing should not throw when the relationship does not exist")
        void validateNotAlreadyFollowing_WhenNotFollowing_ShouldNotThrow() {
            when(followerRepository.existsByUserAndFollower(otherUser, currentUser)).thenReturn(false);

            assertThatCode(() -> followerValidation.validateNotAlreadyFollowing(otherUser, currentUser))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateNotAlreadyFollowing should throw ConflictException when the relationship exists")
        void validateNotAlreadyFollowing_WhenAlreadyFollowing_ShouldThrow() {
            when(followerRepository.existsByUserAndFollower(otherUser, currentUser)).thenReturn(true);

            assertThatThrownBy(() -> followerValidation.validateNotAlreadyFollowing(otherUser, currentUser))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("User is already being followed");
        }

        @Test
        @DisplayName("validateAndGetFollowRelation should return the relationship when it exists")
        void validateAndGetFollowRelation_WhenExists_ShouldReturnRelation() {
            Follower relation = new Follower();
            when(followerRepository.findByUserAndFollower(otherUser, currentUser)).thenReturn(Optional.of(relation));

            assertThat(followerValidation.validateAndGetFollowRelation(otherUser, currentUser)).isSameAs(relation);
        }

        @Test
        @DisplayName("validateAndGetFollowRelation should throw ResourceNotFoundException when it does not exist")
        void validateAndGetFollowRelation_WhenNotFound_ShouldThrow() {
            when(followerRepository.findByUserAndFollower(otherUser, currentUser)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followerValidation.validateAndGetFollowRelation(otherUser, currentUser))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Follow relationship not found");
        }
    }
}
