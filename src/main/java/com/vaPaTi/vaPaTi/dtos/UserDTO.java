package com.vaPaTi.vaPaTi.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private Boolean active;
    private Boolean verified;
    private List<String> categories = new ArrayList<>();
    private UserInfoDTO userInfo;
    private List<BankAccountDTO> bankAccounts = new ArrayList<>();
}
