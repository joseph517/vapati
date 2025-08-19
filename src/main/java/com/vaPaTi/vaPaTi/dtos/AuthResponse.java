package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserInfo userInfo;

    public static class UserInfo {
        public Long userId;
        public String email;
        public String role;
        public String firstName;
        public String lastName;
        public String userName;
        public String fullName;

        public UserInfo() {
        }

        public UserInfo(Long userId, String email, String role, String firstName, String lastName, String userName) {
            this.userId = userId;
            this.email = email;
            this.role = role;
            this.firstName = firstName;
            this.lastName = lastName;
            this.userName = userName;
            this.fullName = firstName + " " + lastName;
        }

        public static UserInfo fromUser(User user) {
            return new UserInfo(
                    user.getId(),
                    user.getUserInfo().getEmail(),
                    user.getRole().getName(),
                    user.getUserInfo().getFirstName(),
                    user.getUserInfo().getLastName(),
                    user.getUserInfo().getUserName()
            );
        }

        public static UserInfo fromUserTokenData(UserTokenData tokenData) {
            return new UserInfo(
                    tokenData.getUserId(),
                    tokenData.getEmail(),
                    tokenData.getRole(),
                    tokenData.getFirstName(),
                    tokenData.getLastName(),
                    tokenData.getUserName()
            );
        }
    }
}