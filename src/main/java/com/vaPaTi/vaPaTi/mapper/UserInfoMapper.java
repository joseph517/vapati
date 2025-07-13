package com.vaPaTi.vaPaTi.mapper;

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
        dto.setPassword(userInfo.getPassword());
        dto.setPhone(userInfo.getPhone());
        dto.setDescription(userInfo.getDescription());
        dto.setProfilePicture(userInfo.getProfilePicture());

        return dto;
    }
}