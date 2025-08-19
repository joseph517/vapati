package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
