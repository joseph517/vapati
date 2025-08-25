package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserInfoDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String userName;
    private String password;
    private String phone;
    private String description;
    private String profilePicture;

}