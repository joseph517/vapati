package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.FollowResponseDto;
import com.vaPaTi.vaPaTi.dtos.FollowersListResponseDto;
import com.vaPaTi.vaPaTi.dtos.UnfollowResponseDto;
import com.vaPaTi.vaPaTi.service.FollowerService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}")
public class FollowerController {

    private final FollowerService followerService;

    public FollowerController(FollowerService followerService) {
        this.followerService = followerService;
    }

    @PostMapping("/follow")
    @Operation(summary = "Follow user", description = "Current user (from the token) follows the user {userId}")
    public ResponseEntity<FollowResponseDto> followUser(@PathVariable Long userId) {
        FollowResponseDto response = followerService.followUser(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/follow")
    @Operation(summary = "Unfollow user", description = "Current user (from the token) unfollows the user {userId}")
    public ResponseEntity<UnfollowResponseDto> unfollowUser(@PathVariable Long userId) {
        UnfollowResponseDto response = followerService.unfollowUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/followers")
    @Operation(summary = "Get followers", description = "Get list of users who follow this user")
    public ResponseEntity<FollowersListResponseDto> getFollowers(@PathVariable Long userId) {
        FollowersListResponseDto response = followerService.getFollowers(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/followers/following")
    @Operation(summary = "Get following", description = "Get list of users that this user follows")
    public ResponseEntity<FollowersListResponseDto> getFollowing(@PathVariable Long userId) {
        FollowersListResponseDto response = followerService.getFollowing(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/followers/is-following/{otherUserId}")
    @Operation(summary = "Check if following", description = "Check if current user follows another user")
    public ResponseEntity<Boolean> isFollowing(@PathVariable Long userId, @PathVariable Long otherUserId) {
        boolean isFollowing = followerService.isFollowing(userId, otherUserId);
        return ResponseEntity.ok(isFollowing);
    }

    @GetMapping("/followers/count")
    @Operation(summary = "Get follower count", description = "Get the number of followers for this user")
    public ResponseEntity<Long> getFollowerCount(@PathVariable Long userId) {
        long count = followerService.getFollowerCount(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/followers/following/count")
    @Operation(summary = "Get following count", description = "Get the number of users this user follows")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long userId) {
        long count = followerService.getFollowingCount(userId);
        return ResponseEntity.ok(count);
    }
}
