package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private Boolean active;
    private Boolean verified;
    private List<String> categories = new ArrayList<>();
    private UserInfoDTO userInfo;
    private List<BankAccountDTO> bankAccounts = new ArrayList<>();
}
