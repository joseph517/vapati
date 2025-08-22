package com.vaPaTi.vaPaTi.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
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
    private Boolean active;
    @Nullable
    private List<Long> categoryIds;

    @Nullable
    private String firstName;
    @Nullable
    private String lastName;
    @Nullable
    private String email;
    @Nullable
    private String userName;
    @Nullable
    private String password;
    @Nullable
    private String phone;
    @Nullable
    private String description;
    @Nullable
    private String profilePicture;
}
