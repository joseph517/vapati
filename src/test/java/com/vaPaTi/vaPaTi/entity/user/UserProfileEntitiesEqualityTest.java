package com.vaPaTi.vaPaTi.entity.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User profile entities equality - Unit Tests")
class UserProfileEntitiesEqualityTest {

    @Nested
    @DisplayName("UserSocialLink")
    class UserSocialLinkEquality {

        @Test
        @DisplayName("Should be equal with the same platform and a different url or id")
        void equals_WithSamePlatform_ShouldBeEqual() {
            // Given
            UserSocialLink first = UserSocialLink.builder()
                    .id(1L).platform(SocialPlatform.GITHUB).url("https://github.com/a").build();
            UserSocialLink second = UserSocialLink.builder()
                    .id(null).platform(SocialPlatform.GITHUB).url("https://github.com/b").build();

            // When & Then
            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        @Test
        @DisplayName("Should not be equal with a different platform")
        void equals_WithDifferentPlatform_ShouldNotBeEqual() {
            // Given
            UserSocialLink first = UserSocialLink.builder().platform(SocialPlatform.GITHUB).url("https://a.com").build();
            UserSocialLink second = UserSocialLink.builder().platform(SocialPlatform.OTHER).url("https://a.com").build();

            // When & Then
            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("Should stay findable in a HashSet after changing its url and id")
        void hashSet_AfterChangingUrlAndId_ShouldStillContainLink() {
            // Given
            UserSocialLink link = UserSocialLink.builder()
                    .platform(SocialPlatform.INSTAGRAM).url("https://instagram.com/a").build();
            Set<UserSocialLink> links = new HashSet<>(Set.of(link));

            // When
            link.setUrl("https://instagram.com/b");
            link.setId(10L);

            // Then
            assertThat(links.contains(link)).isTrue();
            assertThat(links.remove(link)).isTrue();
        }
    }

    @Nested
    @DisplayName("UserTag")
    class UserTagEquality {

        @Test
        @DisplayName("Should be equal with the same tag and a different id")
        void equals_WithSameTag_ShouldBeEqual() {
            // Given
            UserTag first = UserTag.builder().id(1L).tag("music").build();
            UserTag second = UserTag.builder().id(null).tag("music").build();

            // When & Then
            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        @Test
        @DisplayName("Should not be equal with a different tag")
        void equals_WithDifferentTag_ShouldNotBeEqual() {
            // When & Then
            assertThat(UserTag.builder().tag("music").build()).isNotEqualTo(UserTag.builder().tag("art").build());
        }

        @Test
        @DisplayName("Should stay findable in a HashSet after getting an id")
        void hashSet_AfterSettingId_ShouldStillContainTag() {
            // Given
            UserTag tag = UserTag.builder().tag("music").build();
            Set<UserTag> tags = new HashSet<>(Set.of(tag));

            // When
            tag.setId(5L);

            // Then
            assertThat(tags.contains(tag)).isTrue();
            assertThat(tags.remove(tag)).isTrue();
        }
    }

    @Test
    @DisplayName("User built with the builder should have empty, non-null social links and tags")
    void userBuilder_ShouldInitializeSocialLinksAndTags() {
        // When
        User user = User.builder().build();

        // Then
        assertThat(user.getSocialLinks()).isNotNull().isEmpty();
        assertThat(user.getTags()).isNotNull().isEmpty();
    }
}
