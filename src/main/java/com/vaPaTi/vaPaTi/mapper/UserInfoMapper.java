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

        return UserInfo.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .userName(dto.getUserName())
                .password(dto.getPassword())
                .phone(dto.getPhone())
                .description(dto.getDescription())
                .profilePicture(dto.getProfilePicture())
                .build();
    }
}