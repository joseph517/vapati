package com.vaPaTi.vaPaTi.controller.follower;

import com.vaPaTi.vaPaTi.dtos.follower.FollowResponseDTO;
import com.vaPaTi.vaPaTi.dtos.follower.FollowersListResponseDTO;
import com.vaPaTi.vaPaTi.dtos.follower.UnfollowResponseDTO;
import com.vaPaTi.vaPaTi.service.follower.FollowerService;
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
    public ResponseEntity<FollowResponseDTO> followUser(@PathVariable Long userId) {
        FollowResponseDTO response = followerService.followUser(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/follow")
    @Operation(summary = "Unfollow user", description = "Current user (from the token) unfollows the user {userId}")
    public ResponseEntity<UnfollowResponseDTO> unfollowUser(@PathVariable Long userId) {
        UnfollowResponseDTO response = followerService.unfollowUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/followers")
    @Operation(summary = "Get followers", description = "Get list of users who follow this user")
    public ResponseEntity<FollowersListResponseDTO> getFollowers(@PathVariable Long userId) {
        FollowersListResponseDTO response = followerService.getFollowers(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/followers/following")
    @Operation(summary = "Get following", description = "Get list of users that this user follows")
    public ResponseEntity<FollowersListResponseDTO> getFollowing(@PathVariable Long userId) {
        FollowersListResponseDTO response = followerService.getFollowing(userId);
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
