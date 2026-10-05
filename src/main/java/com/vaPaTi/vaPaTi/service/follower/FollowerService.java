package com.vaPaTi.vaPaTi.service.follower;

import com.vaPaTi.vaPaTi.dtos.follower.FollowResponseDTO;
import com.vaPaTi.vaPaTi.dtos.follower.FollowersListResponseDTO;
import com.vaPaTi.vaPaTi.dtos.follower.UnfollowResponseDTO;
import com.vaPaTi.vaPaTi.entity.follower.Follower;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.follower.FollowerMapper;
import com.vaPaTi.vaPaTi.repository.follower.FollowerRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.follower.FollowerValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowerService {

    private final FollowerRepository followerRepository;
    private final FollowerMapper followerMapper;
    private final FollowerValidationService followerValidationService;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * Follows a user by creating a new follower relationship.
     *
     * This method first validates that the current user is not trying to follow themselves, and that both users exist (deleted users are not found), (banned or suspended users are not found either). It then checks if the current user is already following the user to follow, and if so, throws a ConflictException. If not, it creates a new follower relationship and returns a FollowResponseDTO with the result.
     *
     * @param userToFollowId the ID of the user to follow
     * @return a FollowResponseDTO with the result of the follow operation
     * @throws MessageException if the current user is trying to follow themselves
     * @throws ConflictException if the current user is already following the user to follow
     * @throws ResourceNotFoundException if the current user or the user to follow is not found, or the user to follow is blocked
     */
    @Transactional
    public FollowResponseDTO followUser(Long userToFollowId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        followerValidationService.validateNotSelfFollow(userId, userToFollowId);
        User currentUser = followerValidationService.validateAndGetCurrentUser(userId);
        User userToFollow = followerValidationService.validateAndGetUserToFollow(userToFollowId);
        followerValidationService.validateNotAlreadyFollowing(userToFollow, currentUser);

        // Create new follower relationship
        Follower newFollowerRelation = new Follower();
        newFollowerRelation.setUser(userToFollow); // User being followed
        newFollowerRelation.setFollower(currentUser); // User who follows
        newFollowerRelation.setCreatedAt(LocalDateTime.now());

        Follower savedRelation = followerRepository.save(newFollowerRelation);

        return followerMapper.toFollowResponseDto(
                savedRelation,
                "Successfully started following " + userToFollow.getUserInfo().getUserName()
        );
    }


    /**
     * Unfollows a user. This will remove the follower relationship between the current user and the user to unfollow.
     * @param userToUnfollowId the ID of the user to unfollow
     * @return a response DTO with the ID of the unfollowed user and a success message
     * @throws ResourceNotFoundException if the current user, the user to unfollow, or the follow relationship is not found
     * @throws MessageException if the user is trying to unfollow themselves
     */
    @Transactional
    public UnfollowResponseDTO unfollowUser(Long userToUnfollowId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        followerValidationService.validateNotSelfUnfollow(userId, userToUnfollowId);
        User currentUser = followerValidationService.validateAndGetCurrentUser(userId);
        User userToUnfollow = followerValidationService.validateAndGetUserToUnfollow(userToUnfollowId);
        Follower followerRelation = followerValidationService.validateAndGetFollowRelation(userToUnfollow, currentUser);

        // Delete the relationship
        followerRepository.delete(followerRelation);

        return followerMapper.toUnfollowResponseDto(
                userToUnfollowId,
                "Successfully unfollowed " + userToUnfollow.getUserInfo().getUserName()
        );
    }

    /**
     * Get list of followers for a specific user. Banned and suspended followers are left out, except for an ADMIN.
     * @param userId ID of the user whose followers we want to retrieve
     * @return FollowersListResponseDTO with followers list
     */
    public FollowersListResponseDTO getFollowers(Long userId) {
        boolean callerIsAdmin = callerIsAdmin();
        User user = followerValidationService.validateAndGetVisibleUser(userId, callerIsAdmin);

        // Get followers
        List<Follower> followers = callerIsAdmin
                ? followerRepository.findFollowersByUser(user)
                : followerRepository.findVisibleFollowersByUser(user, LocalDateTime.now());

        return followerMapper.toFollowersListResponseDto(userId, followers);
    }

    /**
     * Get list of users that a specific user follows. Banned and suspended users are left out, except for an ADMIN.
     * @param userId ID of the user whose following list we want to retrieve
     * @return FollowersListResponseDTO with following list
     */
    public FollowersListResponseDTO getFollowing(Long userId) {
        boolean callerIsAdmin = callerIsAdmin();
        User user = followerValidationService.validateAndGetVisibleUser(userId, callerIsAdmin);

        // Get following
        List<Follower> following = callerIsAdmin
                ? followerRepository.findFollowingsByFollower(user)
                : followerRepository.findVisibleFollowingsByFollower(user, LocalDateTime.now());

        return followerMapper.toFollowingListResponseDto(userId, following);
    }

    /**
     * Get follower count for a user. Banned and suspended followers don't count, except for an ADMIN.
     * @param userId ID of the user
     * @return number of followers
     */
    public long getFollowerCount(Long userId) {
        boolean callerIsAdmin = callerIsAdmin();
        User user = followerValidationService.validateAndGetVisibleUser(userId, callerIsAdmin);

        return callerIsAdmin
                ? followerRepository.countByUser(user)
                : followerRepository.countVisibleByUser(user, LocalDateTime.now());
    }

    /**
     * Get following count for a user. Banned and suspended users don't count, except for an ADMIN.
     * @param userId ID of the user
     * @return number of users being followed
     */
    public long getFollowingCount(Long userId) {
        boolean callerIsAdmin = callerIsAdmin();
        User user = followerValidationService.validateAndGetVisibleUser(userId, callerIsAdmin);

        return callerIsAdmin
                ? followerRepository.countByFollower(user)
                : followerRepository.countVisibleByFollower(user, LocalDateTime.now());
    }

    public boolean isFollowing(Long currentUserId, Long userToFollowId) {
        return followerValidationService.isFollowing(currentUserId, userToFollowId, callerIsAdmin());
    }

    private boolean callerIsAdmin() {
        return followerValidationService.isAdmin(authenticatedUserService.getAuthenticatedUserId());
    }

}