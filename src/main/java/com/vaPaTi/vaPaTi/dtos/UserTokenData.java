package com.vaPaTi.vaPaTi.dtos;

// Crear esta clase para encapsular los datos del usuario extraídos del token
public class UserTokenData {
    private Long userId;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String userName;

    // Constructor vacío
    public UserTokenData() {}

    // Constructor con todos los parámetros
    public UserTokenData(Long userId, String email, String role, String firstName, String lastName, String userName) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
    }

    // Patrón Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String email;
        private String role;
        private String firstName;
        private String lastName;
        private String userName;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public UserTokenData build() {
            return new UserTokenData(userId, email, role, firstName, lastName, userName);
        }
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

    // Método de conveniencia para obtener el nombre completo
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "UserTokenData{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
}
