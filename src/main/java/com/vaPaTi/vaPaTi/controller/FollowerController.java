package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.FollowRequestDto;
import com.vaPaTi.vaPaTi.dtos.FollowResponseDto;
import com.vaPaTi.vaPaTi.dtos.FollowersListResponseDto;
import com.vaPaTi.vaPaTi.dtos.UnfollowResponseDto;
import com.vaPaTi.vaPaTi.service.FollowerService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/followers")
@Validated
public class FollowerController {

    private final FollowerService followerService;

    public FollowerController(FollowerService followerService) {
        this.followerService = followerService;
    }

    @PostMapping("/follow")
    @Operation(summary = "Follow user", description = "Current user follows another user")
    public ResponseEntity<FollowResponseDto> followUser(
            @PathVariable Long userId,
            @Valid @RequestBody FollowRequestDto followRequestDto) {

        FollowResponseDto response = followerService.followUser(userId, followRequestDto.getUserToFollowId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unfollow")
    @Operation(summary = "Unfollow user", description = "Current user unfollows another user")
    public ResponseEntity<UnfollowResponseDto> unfollowUser(
            @PathVariable Long userId,
            @Valid @RequestBody FollowRequestDto followRequestDto) {

        UnfollowResponseDto response = followerService.unfollowUser(userId, followRequestDto.getUserToFollowId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get followers", description = "Get list of users who follow this user")
    public ResponseEntity<FollowersListResponseDto> getFollowers(@PathVariable Long userId) {
        FollowersListResponseDto response = followerService.getFollowers(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/following")
    @Operation(summary = "Get following", description = "Get list of users that this user follows")
    public ResponseEntity<FollowersListResponseDto> getFollowing(@PathVariable Long userId) {
        FollowersListResponseDto response = followerService.getFollowing(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/is-following/{otherUserId}")
    @Operation(summary = "Check if following", description = "Check if current user follows another user")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable Long userId,
            @PathVariable Long otherUserId) {

        boolean isFollowing = followerService.isFollowing(userId, otherUserId);
        return ResponseEntity.ok(isFollowing);
    }

    @GetMapping("/count")
    @Operation(summary = "Get follower count", description = "Get the number of followers for this user")
    public ResponseEntity<Long> getFollowerCount(@PathVariable Long userId) {
        long count = followerService.getFollowerCount(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/following/count")
    @Operation(summary = "Get following count", description = "Get the number of users this user follows")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long userId) {
        long count = followerService.getFollowingCount(userId);
        return ResponseEntity.ok(count);
    }
}
