package com.vaPaTi.vaPaTi.mapper.user;

import com.vaPaTi.vaPaTi.dtos.user.PublicUserProfileDTO;
import com.vaPaTi.vaPaTi.dtos.user.SocialLinkDTO;
import com.vaPaTi.vaPaTi.dtos.user.UserDTO;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserCategory;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.entity.user.UserSocialLink;
import com.vaPaTi.vaPaTi.entity.user.UserTag;
import com.vaPaTi.vaPaTi.mapper.bankaccount.BankAccountMapper;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    private final UserInfoMapper userInfoMapper;
    private final BankAccountMapper bankAccountMapper;

    public UserMapper(UserInfoMapper userInfoMapper, BankAccountMapper bankAccountMapper) {
        this.userInfoMapper = userInfoMapper;
        this.bankAccountMapper = bankAccountMapper;
    }

    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setVerified(user.isVerified());
        dto.setCategories(mapCategories(user.getUserCategories()));
        dto.setUserInfo(userInfoMapper.toUserInfoDTO(user.getUserInfo()));
        if (user.getBankAccounts() != null && !user.getBankAccounts().isEmpty()) {
            dto.setBankAccounts(
                    user.getBankAccounts().stream()
                            .map(bankAccountMapper::toDto)
                            .toList()
            );
        }

        return dto;
    }

    public UserDTO toUserDTO(User user, Collection<UserSocialLink> socialLinks, Collection<UserTag> tags) {
        UserDTO dto = toUserDTO(user);
        if (dto == null) {
            return null;
        }

        dto.setSocialLinks(mapSocialLinks(socialLinks));
        dto.setTags(mapTags(tags));
        return dto;
    }

    public PublicUserProfileDTO toPublicUserProfileDTO(User user, Collection<UserSocialLink> socialLinks,
                                                       Collection<UserTag> tags) {
        if (user == null) {
            return null;
        }

        PublicUserProfileDTO dto = new PublicUserProfileDTO();
        dto.setId(user.getId());
        dto.setCategories(mapCategories(user.getUserCategories()));
        UserInfo userInfo = user.getUserInfo();
        if (userInfo != null) {
            dto.setFirstName(userInfo.getFirstName());
            dto.setLastName(userInfo.getLastName());
            dto.setUserName(userInfo.getUserName());
            dto.setDescription(userInfo.getDescription());
            dto.setProfilePicture(userInfo.getProfilePicture());
            dto.setCoverImageUrl(userInfo.getCoverImageUrl());
            dto.setIntroVideoUrl(userInfo.getIntroVideoUrl());
            dto.setWebsite(userInfo.getWebsite());
        }
        dto.setSocialLinks(mapSocialLinks(socialLinks));
        dto.setTags(mapTags(tags));

        return dto;
    }

    // Ordered by platform, which is the declaration order of the enum
    private List<SocialLinkDTO> mapSocialLinks(Collection<UserSocialLink> socialLinks) {
        if (socialLinks == null) {
            return new ArrayList<>();
        }

        return socialLinks.stream()
                .sorted(Comparator.comparing(UserSocialLink::getPlatform))
                .map(link -> SocialLinkDTO.builder().platform(link.getPlatform()).url(link.getUrl()).build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // Natural String order
    private List<String> mapTags(Collection<UserTag> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }

        return tags.stream()
                .map(UserTag::getTag)
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<String> mapCategories(Set<UserCategory> userCategories) {
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
