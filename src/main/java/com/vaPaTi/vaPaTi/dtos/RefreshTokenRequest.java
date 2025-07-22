package com.vaPaTi.vaPaTi.dtos;

public class RefreshTokenRequest {

    private String refreshToken;


    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
