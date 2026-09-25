package com.vaPaTi.vaPaTi.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserDTO {
    @Nullable
    private List<Long> categoryIds;

    @Nullable
    @Size(max = 255)
    private String firstName;
    @Nullable
    @Size(max = 255)
    private String lastName;
    @Nullable
    @Size(max = 254)
    private String email;
    @Nullable
    private String userName;
    @Nullable
    private String password;
    @Nullable
    @Size(max = 20)
    private String phone;
    @Nullable
    @Size(max = 255)
    private String description;
    @Nullable
    @Size(max = 255)
    private String profilePicture;

    // Required only when the request changes the password or the email
    @Nullable
    private String currentPassword;
}
