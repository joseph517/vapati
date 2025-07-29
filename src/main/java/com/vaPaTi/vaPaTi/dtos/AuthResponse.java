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

    // Clase interna para encapsular la información del usuario en la respuesta
    public static class UserInfo {
        private Long userId;
        private String email;
        private String role;
        private String firstName;
        private String lastName;
        private String userName;
        private String fullName;

        public UserInfo() {}

        public UserInfo(Long userId, String email, String role, String firstName, String lastName, String userName) {
            this.userId = userId;
            this.email = email;
            this.role = role;
            this.firstName = firstName;
            this.lastName = lastName;
            this.userName = userName;
            this.fullName = firstName + " " + lastName;
        }

        // Factory method para crear desde User entity
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

        // Factory method para crear desde UserTokenData
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

        // Getters y Setters
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
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

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }
}