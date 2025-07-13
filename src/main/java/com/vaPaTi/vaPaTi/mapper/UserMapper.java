package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.UserDTO;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserCategory;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class UserMapper {

    private final UserInfoMapper userInfoMapper;

    public UserMapper(UserInfoMapper userInfoMapper) {
        this.userInfoMapper = userInfoMapper;
    }

    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setIsActive(user.getIsActive());
        dto.setVerified(user.isVerified());
        dto.setCategories(mapCategories(user.getUserCategories()));
        dto.setUserInfo(userInfoMapper.toUserInfoDTO(user.getUserInfo()));

        return dto;
    }

    private List<String> mapCategories(List<UserCategory> userCategories) {
        if (userCategories == null || userCategories.isEmpty()) {
            return new ArrayList<>();
        }

        if (!Hibernate.isInitialized(userCategories)) {
            return new ArrayList<>();
        }

        return userCategories.stream()
                .filter(uc -> uc.getCategory() != null)
                .map(uc -> uc.getCategory().getName())
                .filter(Objects::nonNull)
                .toList();
    }
}