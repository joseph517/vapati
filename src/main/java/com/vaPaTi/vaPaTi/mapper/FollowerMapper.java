package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.FollowResponseDto;
import com.vaPaTi.vaPaTi.dtos.FollowerUserDto;
import com.vaPaTi.vaPaTi.dtos.FollowersListResponseDto;
import com.vaPaTi.vaPaTi.dtos.UnfollowResponseDto;
import com.vaPaTi.vaPaTi.entity.Follower;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class FollowerMapper {

    /**
     * Maps a User entity to FollowerUserDto from a Follower relationship
     * This is used when getting followers (people who follow a specific user)
     */
    public FollowerUserDto toFollowerUserDto(Follower followerRelation) {
        if (followerRelation == null || followerRelation.getFollower() == null ||
                followerRelation.getFollower().getUserInfo() == null) {
            return null;
        }

        User followerUser = followerRelation.getFollower();
        UserInfo userInfo = followerUser.getUserInfo();

        return new FollowerUserDto(
                followerUser.getId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getUserName(),
                userInfo.getProfilePicture(),
                followerRelation.getCreatedAt()
        );
    }

    /**
     * Maps a User entity to FollowerUserDto from a Following relationship
     * This is used when getting following list (people that a specific user follows)
     */
    public FollowerUserDto toFollowingUserDto(Follower followingRelation) {
        if (followingRelation == null || followingRelation.getUser() == null ||
                followingRelation.getUser().getUserInfo() == null) {
            return null;
        }

        User followedUser = followingRelation.getUser();
        UserInfo userInfo = followedUser.getUserInfo();

        return new FollowerUserDto(
                followedUser.getId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getUserName(),
                userInfo.getProfilePicture(),
                followingRelation.getCreatedAt()
        );
    }

    /**
     * Maps a list of Follower entities to FollowerUserDto list
     * Used for followers list (who follows the user)
     */
    public List<FollowerUserDto> toFollowerUserDtoList(List<Follower> followers) {
        if (followers == null || followers.isEmpty()) {
            return new ArrayList<>();
        }

        return followers.stream()
                .map(this::toFollowerUserDto)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Maps a list of Following entities to FollowerUserDto list
     * Used for following list (who the user follows)
     */
    public List<FollowerUserDto> toFollowingUserDtoList(List<Follower> followings) {
        if (followings == null || followings.isEmpty()) {
            return new ArrayList<>();
        }

        return followings.stream()
                .map(this::toFollowingUserDto)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Creates a FollowResponseDto for successful follow operation
     */
    public FollowResponseDto toFollowResponseDto(Follower newFollowerRelation, String message) {
        FollowerUserDto followedUserDto = toFollowingUserDto(newFollowerRelation);
        return new FollowResponseDto(message, true, followedUserDto);
    }

    /**
     * Creates an UnfollowResponseDto for successful unfollow operation
     */
    public UnfollowResponseDto toUnfollowResponseDto(Long unfollowedUserId, String message) {
        return new UnfollowResponseDto(message, true, unfollowedUserId);
    }

    /**
     * Creates a FollowersListResponseDto with followers information
     */
    public FollowersListResponseDto toFollowersListResponseDto(Long userId, List<Follower> followers) {
        List<FollowerUserDto> followerDtos = toFollowerUserDtoList(followers);
        return new FollowersListResponseDto(userId, followerDtos.size(), followerDtos);
    }

    /**
     * Creates a FollowersListResponseDto with following information
     * (reusing the same DTO structure but for following list)
     */
    public FollowersListResponseDto toFollowingListResponseDto(Long userId, List<Follower> followings) {
        List<FollowerUserDto> followingDtos = toFollowingUserDtoList(followings);
        return new FollowersListResponseDto(userId, followingDtos.size(), followingDtos);
    }
}