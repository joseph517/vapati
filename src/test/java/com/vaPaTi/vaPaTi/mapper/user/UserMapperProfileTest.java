package com.vaPaTi.vaPaTi.mapper.user;

import com.vaPaTi.vaPaTi.dtos.user.PublicUserProfileDTO;
import com.vaPaTi.vaPaTi.dtos.user.SocialLinkDTO;
import com.vaPaTi.vaPaTi.dtos.user.UserDTO;
import com.vaPaTi.vaPaTi.dtos.user.UserInfoDTO;
import com.vaPaTi.vaPaTi.entity.user.SocialPlatform;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.entity.user.UserSocialLink;
import com.vaPaTi.vaPaTi.entity.user.UserTag;
import com.vaPaTi.vaPaTi.mapper.bankaccount.BankAccountMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper profile customization Tests")
class UserMapperProfileTest {

    private final UserInfoMapper userInfoMapper = new UserInfoMapper();
    private final UserMapper userMapper = new UserMapper(userInfoMapper, new BankAccountMapper());

    private User user() {
        UserInfo userInfo = UserInfo.builder()
                .firstName("John")
                .userName("johndoe")
                .coverImageUrl("https://example.com/cover.png")
                .introVideoUrl("https://example.com/intro.mp4")
                .website("https://example.com")
                .build();
        return User.builder().id(7L).userInfo(userInfo).build();
    }

    private UserSocialLink link(SocialPlatform platform, String url) {
        return UserSocialLink.builder().platform(platform).url(url).build();
    }

    private UserTag tag(String value) {
        return UserTag.builder().tag(value).build();
    }

    @Test
    @DisplayName("toUserDTO: links come out ordered by the enum even if they arrive unordered")
    void toUserDTO_ShouldOrderLinksByPlatform() {
        UserDTO dto = userMapper.toUserDTO(user(), List.of(
                link(SocialPlatform.OTHER, "https://o.com"),
                link(SocialPlatform.GITHUB, "https://github.com/a"),
                link(SocialPlatform.INSTAGRAM, "https://instagram.com/a")), List.of());

        assertThat(dto.getSocialLinks()).extracting(SocialLinkDTO::getPlatform)
                .containsExactly(SocialPlatform.INSTAGRAM, SocialPlatform.GITHUB, SocialPlatform.OTHER);
        assertThat(dto.getSocialLinks().get(0).getUrl()).isEqualTo("https://instagram.com/a");
    }

    @Test
    @DisplayName("toUserDTO: tags come out in natural order")
    void toUserDTO_ShouldOrderTagsNaturally() {
        UserDTO dto = userMapper.toUserDTO(user(), List.of(), List.of(tag("music"), tag("art"), tag("zen"), tag("éxito")));

        assertThat(dto.getTags()).containsExactly("art", "music", "zen", "éxito");
    }

    @Test
    @DisplayName("toUserDTO: null collections give empty lists")
    void toUserDTO_WithNullCollections_ShouldReturnEmptyLists() {
        UserDTO dto = userMapper.toUserDTO(user(), null, null);

        assertThat(dto.getSocialLinks()).isEmpty();
        assertThat(dto.getTags()).isEmpty();
    }

    @Test
    @DisplayName("toUserDTO(user): leaves socialLinks and tags null")
    void toUserDTO_WithoutCollections_ShouldLeaveThemNull() {
        UserDTO dto = userMapper.toUserDTO(user());

        assertThat(dto.getSocialLinks()).isNull();
        assertThat(dto.getTags()).isNull();
    }

    @Test
    @DisplayName("toUserDTO: null user returns null")
    void toUserDTO_WithNullUser_ShouldReturnNull() {
        assertThat(userMapper.toUserDTO(null, List.of(), List.of())).isNull();
    }

    @Test
    @DisplayName("The three URLs reach UserInfoDTO")
    void toUserInfoDTO_ShouldMapUrls() {
        UserInfoDTO dto = userInfoMapper.toUserInfoDTO(user().getUserInfo());

        assertThat(dto.getCoverImageUrl()).isEqualTo("https://example.com/cover.png");
        assertThat(dto.getIntroVideoUrl()).isEqualTo("https://example.com/intro.mp4");
        assertThat(dto.getWebsite()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("toPublicUserProfileDTO: maps URLs, ordered links and tags")
    void toPublicUserProfileDTO_ShouldMapProfileFields() {
        PublicUserProfileDTO dto = userMapper.toPublicUserProfileDTO(user(),
                List.of(link(SocialPlatform.TWITCH, "https://twitch.tv/a"), link(SocialPlatform.X, "https://x.com/a")),
                List.of(tag("b"), tag("a")));

        assertThat(dto.getCoverImageUrl()).isEqualTo("https://example.com/cover.png");
        assertThat(dto.getIntroVideoUrl()).isEqualTo("https://example.com/intro.mp4");
        assertThat(dto.getWebsite()).isEqualTo("https://example.com");
        assertThat(dto.getSocialLinks()).extracting(SocialLinkDTO::getPlatform)
                .containsExactly(SocialPlatform.X, SocialPlatform.TWITCH);
        assertThat(dto.getTags()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("toPublicUserProfileDTO: null collections give empty lists")
    void toPublicUserProfileDTO_WithNullCollections_ShouldReturnEmptyLists() {
        PublicUserProfileDTO dto = userMapper.toPublicUserProfileDTO(user(), null, null);

        assertThat(dto.getSocialLinks()).isEmpty();
        assertThat(dto.getTags()).isEmpty();
    }
}
