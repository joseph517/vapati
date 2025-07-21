package com.vaPaTi.vaPaTi.dtos;

import java.util.List;

public class FollowersListResponseDto {

    private Long userId;
    private int totalFollowers;
    private List<FollowerUserDto> followers;

    public FollowersListResponseDto(Long userId, int totalFollowers, List<FollowerUserDto> followers) {
        this.userId = userId;
        this.totalFollowers = totalFollowers;
        this.followers = followers;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getTotalFollowers() {
        return totalFollowers;
    }

    public void setTotalFollowers(int totalFollowers) {
        this.totalFollowers = totalFollowers;
    }

    public List<FollowerUserDto> getFollowers() {
        return followers;
    }

    public void setFollowers(List<FollowerUserDto> followers) {
        this.followers = followers;
    }
}
