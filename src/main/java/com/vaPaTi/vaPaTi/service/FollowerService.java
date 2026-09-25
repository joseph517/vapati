package com.vaPaTi.vaPaTi.service;

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
import com.vaPaTi.vaPaTi.validation.FollowerValidation;
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
    private final FollowerValidation followerValidation;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * Follows a user by creating a new follower relationship.
     *
     * This method first validates that the current user is not trying to follow themselves, and that both users exist (deleted users are not found), and that the user to follow is not banned or suspended. It then checks if the current user is already following the user to follow, and if so, throws a ConflictException. If not, it creates a new follower relationship and returns a FollowResponseDTO with the result.
     *
     * @param userToFollowId the ID of the user to follow
     * @return a FollowResponseDTO with the result of the follow operation
     * @throws MessageException if the current user is trying to follow themselves, or the user to follow is banned or suspended
     * @throws ConflictException if the current user is already following the user to follow
     * @throws ResourceNotFoundException if the current user or the user to follow is not found
     */
    @Transactional
    public FollowResponseDTO followUser(Long userToFollowId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        followerValidation.validateNotSelfFollow(userId, userToFollowId);
        User currentUser = followerValidation.validateAndGetCurrentUser(userId);
        User userToFollow = followerValidation.validateAndGetUserToFollow(userToFollowId);
        followerValidation.validateNotSanctioned(userToFollow);
        followerValidation.validateNotAlreadyFollowing(userToFollow, currentUser);

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

        followerValidation.validateNotSelfUnfollow(userId, userToUnfollowId);
        User currentUser = followerValidation.validateAndGetCurrentUser(userId);
        User userToUnfollow = followerValidation.validateAndGetUserToUnfollow(userToUnfollowId);
        Follower followerRelation = followerValidation.validateAndGetFollowRelation(userToUnfollow, currentUser);

        // Delete the relationship
        followerRepository.delete(followerRelation);

        return followerMapper.toUnfollowResponseDto(
                userToUnfollowId,
                "Successfully unfollowed " + userToUnfollow.getUserInfo().getUserName()
        );
    }

    /**
     * Get list of followers for a specific user
     * @param userId ID of the user whose followers we want to retrieve
     * @return FollowersListResponseDTO with followers list
     */
    public FollowersListResponseDTO getFollowers(Long userId) {
        User user = followerValidation.validateAndGetUser(userId);

        // Get followers
        List<Follower> followers = followerRepository.findFollowersByUser(user);

        return followerMapper.toFollowersListResponseDto(userId, followers);
    }

    /**
     * Get list of users that a specific user follows
     * @param userId ID of the user whose following list we want to retrieve
     * @return FollowersListResponseDTO with following list
     */
    public FollowersListResponseDTO getFollowing(Long userId) {
        User user = followerValidation.validateAndGetUser(userId);

        // Get following
        List<Follower> following = followerRepository.findFollowingsByFollower(user);

        return followerMapper.toFollowingListResponseDto(userId, following);
    }

    /**
     * Get follower count for a user
     * @param userId ID of the user
     * @return number of followers
     */
    public long getFollowerCount(Long userId) {
        User user = followerValidation.validateAndGetUser(userId);

        return followerRepository.countByUser(user);
    }

    /**
     * Get following count for a user
     * @param userId ID of the user
     * @return number of users being followed
     */
    public long getFollowingCount(Long userId) {
        User user = followerValidation.validateAndGetUser(userId);

        return followerRepository.countByFollower(user);
    }

    public boolean isFollowing(Long currentUserId, Long userToFollowId) {
        return followerValidation.isFollowing(currentUserId, userToFollowId);
    }

}