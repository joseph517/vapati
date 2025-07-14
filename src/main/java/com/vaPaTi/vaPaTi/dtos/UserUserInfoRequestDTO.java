package com.vaPaTi.vaPaTi.dtos;

public class UserUserInfoRequestDTO {

    private CreateUserDTO user;
    private CreateUserInfoDTO userInfo;

    // Getters y setters
    public CreateUserDTO getUser() {
        return user;
    }

    public void setUser(CreateUserDTO user) {
        this.user = user;
    }

    public CreateUserInfoDTO getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(CreateUserInfoDTO userInfo) {
        this.userInfo = userInfo;
    }
}
