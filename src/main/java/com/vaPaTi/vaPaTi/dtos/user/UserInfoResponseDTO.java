package com.vaPaTi.vaPaTi.dtos;

import lombok.Data;

@Data
public class UserInfoResponseDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String userName;
    private String password;
    private String phone;
    private String description;
    private String profilePicture;
}
