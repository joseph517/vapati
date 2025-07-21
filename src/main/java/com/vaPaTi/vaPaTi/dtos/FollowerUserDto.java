package com.vaPaTi.vaPaTi.dtos;

import java.time.LocalDateTime;

// Response DTO for displaying user basic info in follower lists
public class FollowerUserDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String userName;
    private String profilePicture;
    private LocalDateTime followedAt; // When this follow relationship was created

    public FollowerUserDto(Long id, String firstName, String lastName, String userName, String profilePicture, LocalDateTime followedAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.profilePicture = profilePicture;
        this.followedAt = followedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public LocalDateTime getFollowedAt() {
        return followedAt;
    }

    public void setFollowedAt(LocalDateTime followedAt) {
        this.followedAt = followedAt;
    }
}
