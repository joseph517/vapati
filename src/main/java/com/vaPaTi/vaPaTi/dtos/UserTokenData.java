package com.vaPaTi.vaPaTi.dtos;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
public class UserTokenData {
    @NotNull
    private Long userId;
    @NotNull
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String userName;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
