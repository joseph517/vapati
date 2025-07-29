package com.vaPaTi.vaPaTi.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserDTO {
    private Boolean active;
    @Nullable
    private List<Long> categoryIds = new ArrayList<>();

    // Campos de UserInfo
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
