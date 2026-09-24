package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.entity.Follower;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.FollowerRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowerValidation {

    private final UserRepository userRepository;
    private final FollowerRepository followerRepository;

    /**
     * Check if current user follows another user
     * @param currentUserId ID of the current user
     * @param otherUserId ID of the other user
     * @return true if current user follows the other user
     */
    public boolean isFollowing(@NotNull Long currentUserId, Long otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            return false; // User cannot follow themselves
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found with ID: " + currentUserId));

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Other user not found with ID: " + otherUserId));

        return followerRepository.existsByUserAndFollower(otherUser, currentUser);
    }

    /**
     * Validates that the current user is not trying to follow themselves.
     * @param currentUserId ID of the authenticated user
     * @param targetUserId ID of the user to follow
     * @throws MessageException if both IDs are the same
     */
    public void validateNotSelfFollow(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new MessageException("Users cannot follow themselves");
        }
    }

    /**
     * Validates that the current user is not trying to unfollow themselves.
     * @param currentUserId ID of the authenticated user
     * @param targetUserId ID of the user to unfollow
     * @throws MessageException if both IDs are the same
     */
    public void validateNotSelfUnfollow(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new MessageException("Users cannot unfollow themselves");
        }
    }

    /**
     * Loads the authenticated user (deleted users are not found).
     * @param userId ID of the authenticated user
     * @return the current user
     * @throws ResourceNotFoundException if the user does not exist
     */
    public User validateAndGetCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found with ID: " + userId));
    }

    /**
     * Loads the user to follow (deleted users are not found).
     * @param userId ID of the user to follow
     * @return the user to follow
     * @throws ResourceNotFoundException if the user does not exist
     */
    public User validateAndGetUserToFollow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User to follow not found with ID: " + userId));
    }

    /**
     * Loads the user to unfollow (deleted users are not found).
     * @param userId ID of the user to unfollow
     * @return the user to unfollow
     * @throws ResourceNotFoundException if the user does not exist
     */
    public User validateAndGetUserToUnfollow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User to unfollow not found with ID: " + userId));
    }

    /**
     * Loads the user whose followers or followings are listed or counted (deleted users are not found).
     * @param userId ID of the user
     * @return the user
     * @throws ResourceNotFoundException if the user does not exist
     */
    public User validateAndGetUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Validates that the current user does not follow the user to follow yet.
     * @param userToFollow the user being followed
     * @param currentUser the user who follows
     * @throws ConflictException if the relationship already exists
     */
    public void validateNotAlreadyFollowing(User userToFollow, User currentUser) {
        if (followerRepository.existsByUserAndFollower(userToFollow, currentUser)) {
            throw new ConflictException("User is already being followed");
        }
    }

    /**
     * Loads the follow relationship between the current user and the user to unfollow.
     * @param userToUnfollow the user being followed
     * @param currentUser the user who follows
     * @return the follow relationship
     * @throws ResourceNotFoundException if the relationship does not exist
     */
    public Follower validateAndGetFollowRelation(User userToUnfollow, User currentUser) {
        return followerRepository.findByUserAndFollower(userToUnfollow, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Follow relationship not found"));
    }
}
