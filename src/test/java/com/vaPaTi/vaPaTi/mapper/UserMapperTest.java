package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.PublicUserProfileDTO;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserCategory;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper Tests")
class UserMapperTest {

    // toPublicUserProfileDTO doesn't use the nested mappers
    private final UserMapper userMapper = new UserMapper(null, null);

    @Test
    @DisplayName("toPublicUserProfileDTO: maps only the public fields")
    void toPublicUserProfileDTO_ShouldMapPublicFields() {
        UserInfo userInfo = UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .description("About me")
                .profilePicture("pic.jpg")
                .build();
        User user = User.builder()
                .id(7L)
                .verified(true)
                .userInfo(userInfo)
                .userCategories(new HashSet<>(Set.of(
                        UserCategory.builder().category(Category.builder().id(1L).name("Technology").build()).build()
                )))
                .build();

        PublicUserProfileDTO dto = userMapper.toPublicUserProfileDTO(user);

        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getCategories()).containsExactly("Technology");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getUserName()).isEqualTo("johndoe");
        assertThat(dto.getDescription()).isEqualTo("About me");
        assertThat(dto.getProfilePicture()).isEqualTo("pic.jpg");
    }

    @Test
    @DisplayName("PublicUserProfileDTO has no private fields (email, phone, verified, active, bankAccounts, userInfo)")
    void publicUserProfileDTO_ShouldNotExposePrivateFields() {
        assertThat(Arrays.stream(PublicUserProfileDTO.class.getDeclaredFields()).map(Field::getName))
                .containsExactlyInAnyOrder("id", "categories", "firstName", "lastName", "userName", "description", "profilePicture");
    }

    @Test
    @DisplayName("toPublicUserProfileDTO: null user returns null")
    void toPublicUserProfileDTO_WithNullUser_ShouldReturnNull() {
        assertThat(userMapper.toPublicUserProfileDTO(null)).isNull();
    }
}
