package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.FollowResponseDto;
import com.vaPaTi.vaPaTi.dtos.FollowersListResponseDto;
import com.vaPaTi.vaPaTi.dtos.UnfollowResponseDto;
import com.vaPaTi.vaPaTi.entity.Follower;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.FollowerMapper;
import com.vaPaTi.vaPaTi.repository.FollowerRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.validation.FollowerValidation;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FollowerService {

    private final FollowerRepository followerRepository;
    private final UserRepository userRepository;
    private final FollowerMapper followerMapper;
    private final FollowerValidation followerValidation;

    private static final String USER_NOT_FOUND = "User not found with ID: ";

    public FollowerService(FollowerRepository followerRepository,
                           UserRepository userRepository,
                           FollowerMapper followerMapper,
                           FollowerValidation followerValidation) {
        this.followerRepository = followerRepository;
        this.userRepository = userRepository;
        this.followerMapper = followerMapper;
        this.followerValidation = followerValidation;
    }

    /**
     * Follow a user
     * @param currentUserId ID of the user who wants to follow
     * @param userToFollowId ID of the user to be followed
     * @return FollowResponseDto with operation result
     */
    @Transactional
    public FollowResponseDto followUser(@NotNull Long currentUserId, Long userToFollowId) {
        // Validate that user is not trying to follow themselves
        if (currentUserId.equals(userToFollowId)) {
            throw new IllegalArgumentException("Users cannot follow themselves");
        }

        // Get current user
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found with ID: " + currentUserId));

        // Validate current user is active
        if (!currentUser.isActive()) {
            throw new IllegalStateException("Inactive users cannot follow other users");
        }

        // Get user to follow
        User userToFollow = userRepository.findById(userToFollowId)
                .orElseThrow(() -> new EntityNotFoundException("User to follow not found with ID: " + userToFollowId));

        // Validate user to follow is active
        if (!userToFollow.isActive()) {
            throw new IllegalStateException("Cannot follow inactive users");
        }

        // Check if already following
        if (followerRepository.existsByUserAndFollower(userToFollow, currentUser)) {
            throw new IllegalStateException("User is already being followed");
        }

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
     * Unfollow a user
     * @param currentUserId ID of the user who wants to unfollow
     * @param userToUnfollowId ID of the user to be unfollowed
     * @return UnfollowResponseDto with operation result
     */
    @Transactional
    public UnfollowResponseDto unfollowUser(@NotNull Long currentUserId, Long userToUnfollowId) {
        // Validate that user is not trying to unfollow themselves
        if (currentUserId.equals(userToUnfollowId)) {
            throw new IllegalArgumentException("Users cannot unfollow themselves");
        }

        // Get current user
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found with ID: " + currentUserId));

        // Get user to unfollow
        User userToUnfollow = userRepository.findById(userToUnfollowId)
                .orElseThrow(() -> new EntityNotFoundException("User to unfollow not found with ID: " + userToUnfollowId));

        // Find the follower relationship
        Follower followerRelation = followerRepository.findByUserAndFollower(userToUnfollow, currentUser)
                .orElseThrow(() -> new EntityNotFoundException("Follow relationship not found"));

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
     * @return FollowersListResponseDto with followers list
     */
    public FollowersListResponseDto getFollowers(Long userId) {
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND + userId));

        // Get followers
        List<Follower> followers = followerRepository.findFollowersByUser(user);

        return followerMapper.toFollowersListResponseDto(userId, followers);
    }

    /**
     * Get list of users that a specific user follows
     * @param userId ID of the user whose following list we want to retrieve
     * @return FollowersListResponseDto with following list
     */
    public FollowersListResponseDto getFollowing(Long userId) {
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND + userId));

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND + userId));

        return followerRepository.countByUser(user);
    }

    /**
     * Get following count for a user
     * @param userId ID of the user
     * @return number of users being followed
     */
    public long getFollowingCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND + userId));

        return followerRepository.countByFollower(user);
    }

    public boolean isFollowing(Long currentUserId, Long userToFollowId) {
        return followerValidation.isFollowing(currentUserId, userToFollowId);
    }

}