package com.vaPaTi.vaPaTi.mapper.follower;

import com.vaPaTi.vaPaTi.dtos.follower.FollowResponseDTO;
import com.vaPaTi.vaPaTi.dtos.follower.FollowerUserDTO;
import com.vaPaTi.vaPaTi.dtos.follower.FollowersListResponseDTO;
import com.vaPaTi.vaPaTi.dtos.follower.UnfollowResponseDTO;
import com.vaPaTi.vaPaTi.entity.follower.Follower;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class FollowerMapper {

    /**
     * Maps a User entity to FollowerUserDTO from a Follower relationship
     * This is used when getting followers (people who follow a specific user)
     */
    public FollowerUserDTO toFollowerUserDto(Follower followerRelation) {
        if (followerRelation == null || followerRelation.getFollower() == null ||
                followerRelation.getFollower().getUserInfo() == null) {
            return null;
        }

        User followerUser = followerRelation.getFollower();
        UserInfo userInfo = followerUser.getUserInfo();

        return new FollowerUserDTO(
                followerUser.getId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getUserName(),
                userInfo.getProfilePicture(),
                followerRelation.getCreatedAt()
        );
    }

    /**
     * Maps a User entity to FollowerUserDTO from a Following relationship
     * This is used when getting following list (people that a specific user follows)
     */
    public FollowerUserDTO toFollowingUserDto(Follower followingRelation) {
        if (followingRelation == null || followingRelation.getUser() == null ||
                followingRelation.getUser().getUserInfo() == null) {
            return null;
        }

        User followedUser = followingRelation.getUser();
        UserInfo userInfo = followedUser.getUserInfo();

        return new FollowerUserDTO(
                followedUser.getId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getUserName(),
                userInfo.getProfilePicture(),
                followingRelation.getCreatedAt()
        );
    }

    /**
     * Maps a list of Follower entities to FollowerUserDTO list
     * Used for followers list (who follows the user)
     */
    public List<FollowerUserDTO> toFollowerUserDtoList(List<Follower> followers) {
        if (followers == null || followers.isEmpty()) {
            return new ArrayList<>();
        }

        return followers.stream()
                .map(this::toFollowerUserDto)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Maps a list of Following entities to FollowerUserDTO list
     * Used for following list (who the user follows)
     */
    public List<FollowerUserDTO> toFollowingUserDtoList(List<Follower> followings) {
        if (followings == null || followings.isEmpty()) {
            return new ArrayList<>();
        }

        return followings.stream()
                .map(this::toFollowingUserDto)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Creates a FollowResponseDTO for successful follow operation
     */
    public FollowResponseDTO toFollowResponseDto(Follower newFollowerRelation, String message) {
        FollowerUserDTO followedUserDto = toFollowingUserDto(newFollowerRelation);
        return new FollowResponseDTO(message, true, followedUserDto);
    }

    /**
     * Creates an UnfollowResponseDTO for successful unfollow operation
     */
    public UnfollowResponseDTO toUnfollowResponseDto(Long unfollowedUserId, String message) {
        return new UnfollowResponseDTO(message, true, unfollowedUserId);
    }

    /**
     * Creates a FollowersListResponseDTO with followers information
     */
    public FollowersListResponseDTO toFollowersListResponseDto(Long userId, List<Follower> followers) {
        List<FollowerUserDTO> followerDtos = toFollowerUserDtoList(followers);
        return new FollowersListResponseDTO(userId, followerDtos.size(), followerDtos);
    }

    /**
     * Creates a FollowersListResponseDTO with following information
     * (reusing the same DTO structure but for following list)
     */
    public FollowersListResponseDTO toFollowingListResponseDto(Long userId, List<Follower> followings) {
        List<FollowerUserDTO> followingDtos = toFollowingUserDtoList(followings);
        return new FollowersListResponseDTO(userId, followingDtos.size(), followingDtos);
    }
}