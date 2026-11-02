package com.vaPaTi.vaPaTi.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserInfoDTO {
    @NotBlank
    @Size(max = 255)
    private String firstName;

    @NotBlank
    @Size(max = 255)
    private String lastName;

    // The format is checked in UserAccountValidationService
    @NotBlank
    @Size(max = 254)
    private String email;

    // Length and allowed characters are checked in UserAccountValidationService
    @NotBlank
    private String userName;

    // Password rules are checked in UserAccountValidationService
    @NotBlank
    private String password;

    @NotBlank
    @Size(max = 20)
    private String phone;

    @NotNull
    @Size(max = 255)
    private String description;

    @Size(max = 255)
    private String profilePicture;

}
