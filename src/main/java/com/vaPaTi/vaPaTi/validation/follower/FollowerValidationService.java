package com.vaPaTi.vaPaTi.validation.follower;

import com.vaPaTi.vaPaTi.entity.follower.Follower;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.follower.FollowerRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowerValidationService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final FollowerRepository followerRepository;
    private final AccountStatusValidationService accountStatusValidationService;

    /**
     * Check if current user follows another user. For a non-admin, a banned or suspended user is not found.
     * @param currentUserId ID of the current user
     * @param otherUserId ID of the other user
     * @param callerIsAdmin whether the authenticated user is an ADMIN
     * @return true if current user follows the other user
     */
    public boolean isFollowing(@NotNull Long currentUserId, Long otherUserId, boolean callerIsAdmin) {
        if (currentUserId.equals(otherUserId)) {
            return false; // User cannot follow themselves
        }

        User currentUser = userRepository.findById(currentUserId)
                .filter(user -> callerIsAdmin || !accountStatusValidationService.isBlocked(user))
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found with ID: " + currentUserId));

        User otherUser = userRepository.findById(otherUserId)
                .filter(user -> callerIsAdmin || !accountStatusValidationService.isBlocked(user))
                .orElseThrow(() -> new ResourceNotFoundException("Other user not found with ID: " + otherUserId));

        return followerRepository.existsByUserAndFollower(otherUser, currentUser);
    }

    /**
     * Whether the user is an ADMIN, read from the database. False for a null id (anonymous).
     * @param callerId ID of the authenticated user
     * @return true if the user has the ADMIN role
     */
    public boolean isAdmin(Long callerId) {
        return callerId != null && userRepository.existsByIdAndRole_Name(callerId, ADMIN_ROLE);
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
     * Loads the user to follow. Deleted, banned and suspended users are not found, so the sanction is not revealed.
     * @param userId ID of the user to follow
     * @return the user to follow
     * @throws ResourceNotFoundException if the user does not exist or is blocked
     */
    public User validateAndGetUserToFollow(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !accountStatusValidationService.isBlocked(user))
                .orElseThrow(() -> new ResourceNotFoundException("User to follow not found with ID: " + userId));
    }

    /**
     * Loads the user to unfollow. Deleted, banned and suspended users are not found, so the sanction is not revealed.
     * @param userId ID of the user to unfollow
     * @return the user to unfollow
     * @throws ResourceNotFoundException if the user does not exist or is blocked
     */
    public User validateAndGetUserToUnfollow(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !accountStatusValidationService.isBlocked(user))
                .orElseThrow(() -> new ResourceNotFoundException("User to unfollow not found with ID: " + userId));
    }

    /**
     * Loads the user whose followers or followings are listed or counted (deleted users are not found).
     * For a non-admin, a banned or suspended user is not found either.
     * @param userId ID of the user
     * @param callerIsAdmin whether the authenticated user is an ADMIN
     * @return the user
     * @throws ResourceNotFoundException if the user does not exist, or is blocked and the caller is not an ADMIN
     */
    public User validateAndGetVisibleUser(Long userId, boolean callerIsAdmin) {
        // One SELECT with userInfo, role and verificationRequest, instead of findById plus the inverse @OneToOne
        return userRepository.findByIdForRequest(userId)
                .filter(user -> callerIsAdmin || !accountStatusValidationService.isBlocked(user))
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
