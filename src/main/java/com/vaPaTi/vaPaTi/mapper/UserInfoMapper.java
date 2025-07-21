package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.CreateUserInfoDTO;
import com.vaPaTi.vaPaTi.dtos.UserInfoDTO;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import org.springframework.stereotype.Component;

@Component
public class UserInfoMapper {

    public UserInfoDTO toUserInfoDTO(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }

        UserInfoDTO dto = new UserInfoDTO();
        dto.setFirstName(userInfo.getFirstName());
        dto.setLastName(userInfo.getLastName());
        dto.setEmail(userInfo.getEmail());
        dto.setUserName(userInfo.getUserName());
        dto.setPhone(userInfo.getPhone());
        dto.setDescription(userInfo.getDescription());
        dto.setProfilePicture(userInfo.getProfilePicture());

        return dto;
    }

    public UserInfo fromCreateUserInfoDTO(CreateUserInfoDTO dto) {
        if (dto == null) {
            return null;
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setFirstName(dto.getFirstName());
        userInfo.setLastName(dto.getLastName());
        userInfo.setEmail(dto.getEmail());
        userInfo.setUserName(dto.getUserName());
        userInfo.setPassword(dto.getPassword());
        userInfo.setPhone(dto.getPhone());
        userInfo.setDescription(dto.getDescription());
        userInfo.setProfilePicture(dto.getProfilePicture());

        return userInfo;
    }
}