package com.vaPaTi.vaPaTi.dtos;

import java.util.ArrayList;
import java.util.List;

public class UserDTO {

    private Long id;
    private Boolean isActive;
    private Boolean isVerified;
    private List<String> categories = new ArrayList<>();
    private UserInfoDTO userInfo;
    private List<BankAccountDTO> bankAccounts = new ArrayList<>();

    // Getters y setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Boolean getIsActive() {
        return isActive;
    }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    public List<String> getCategories() {
        return categories;
    }
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public UserInfoDTO getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfoDTO userInfo) {
        this.userInfo = userInfo;
    }

    public Boolean getVerified() {
        return isVerified;
    }

    public void setVerified(Boolean verified) {
        isVerified = verified;
    }

    public List<BankAccountDTO> getBankAccounts() {
        return bankAccounts;
    }

    public void setBankAccounts(List<BankAccountDTO> bankAccounts) {
        this.bankAccounts = bankAccounts;
    }
}
