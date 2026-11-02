package com.vaPaTi.vaPaTi.dtos.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vaPaTi.vaPaTi.dtos.bankaccount.BankAccountDTO;
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
    private Boolean verified;
    private List<String> categories = new ArrayList<>();
    private UserInfoDTO userInfo;
    private List<BankAccountDTO> bankAccounts = new ArrayList<>();

    // Only GET /api/users/{id} and PUT /api/users/update load them; the user lists and the creation response omit the keys
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SocialLinkDTO> socialLinks;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> tags;
}
