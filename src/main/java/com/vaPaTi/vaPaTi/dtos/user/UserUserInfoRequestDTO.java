package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUserInfoRequestDTO {
    @NotNull
    @Valid
    private CreateUserDTO user;

    @NotNull
    @Valid
    private CreateUserInfoDTO userInfo;
}
