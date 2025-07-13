package com.vaPaTi.vaPaTi.dtos;

public class UserUserInfoRequestDTO {

    private CreateUserDTO user;
    private UserInfoDTO userInfo;

    // Getters y setters
    public CreateUserDTO getUser() {
        return user;
    }

    public void setUser(CreateUserDTO user) {
        this.user = user;
    }

    public UserInfoDTO getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfoDTO userInfo) {
        this.userInfo = userInfo;
    }
}
