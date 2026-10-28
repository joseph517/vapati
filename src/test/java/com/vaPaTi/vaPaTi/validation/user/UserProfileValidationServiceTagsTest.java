package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserTag;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.common.UrlValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileValidationService - Tags Tests")
class UserProfileValidationServiceTagsTest {

    private static final String INVALID_CHARACTERS = "Tag contains invalid characters. Allowed: letters, digits, spaces and '-'";

    @Mock
    private TextValidationService textValidationService;

    @Mock
    private UrlValidationService urlValidationService;

    @InjectMocks
    private UserProfileValidationService userProfileValidationService;

    private User user;
    private UserTag art;
    private UserTag music;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).build();
        art = UserTag.builder().id(10L).user(user).tag("arte").build();
        music = UserTag.builder().id(11L).user(user).tag("música").build();
        user.setTags(new HashSet<>(Set.of(art, music)));
    }

    private Set<String> tagNames() {
        return user.getTags().stream().map(UserTag::getTag).collect(Collectors.toSet());
    }

    private void assertTagsUnchanged() {
        assertThat(user.getTags()).containsExactlyInAnyOrder(art, music);
        assertThat(tagNames()).containsExactlyInAnyOrder("arte", "música");
    }

    private static List<String> distinctTags(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> "tag" + i).toList();
    }

    @Nested
    @DisplayName("Normalization")
    class Normalization {

        @Test
        @DisplayName("Should trim, drop the leading '#' and lowercase")
        void replaceTags_WithHashAndSpaces_ShouldNormalize() {
            // When
            userProfileValidationService.replaceTags(user, List.of("  #Arte "));

            // Then
            assertThat(tagNames()).containsExactly("arte");
        }

        @Test
        @DisplayName("Should drop every leading '#' and collapse inner spaces")
        void replaceTags_WithSeveralHashesAndInnerSpaces_ShouldCollapse() {
            // When
            userProfileValidationService.replaceTags(user, List.of("##música   en   vivo"));

            // Then
            assertThat(tagNames()).containsExactly("música en vivo");
        }

        @Test
        @DisplayName("Should merge tags that are equal after normalizing")
        void replaceTags_WithDuplicatesAfterNormalizing_ShouldMerge() {
            // Given
            user.setTags(new HashSet<>());

            // When
            userProfileValidationService.replaceTags(user, List.of("Arte", "arte", "#ARTE"));

            // Then
            assertThat(user.getTags()).hasSize(1);
            assertThat(tagNames()).containsExactly("arte");
        }

        @ParameterizedTest
        @ValueSource(strings = {"podcast-semanal", "日本", "año 2026"})
        @DisplayName("Should accept letters of any script, digits, spaces and '-'")
        void replaceTags_WithAllowedCharacters_ShouldAccept(String tag) {
            // When
            userProfileValidationService.replaceTags(user, List.of(tag));

            // Then
            assertThat(tagNames()).containsExactly(tag);
        }
    }

    @Nested
    @DisplayName("Validation errors")
    class ValidationErrors {

        @Test
        @DisplayName("Should accept 12 elements with 10 distinct tags")
        void replaceTags_WithTwelveElementsTenDistinct_ShouldAccept() {
            // Given
            List<String> tags = new ArrayList<>(distinctTags(10));
            tags.add("TAG1");
            tags.add("#tag2");

            // When
            userProfileValidationService.replaceTags(user, tags);

            // Then
            assertThat(user.getTags()).hasSize(10);
        }

        @Test
        @DisplayName("Should reject 11 distinct tags")
        void replaceTags_WithElevenDistinct_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceTags(user, distinctTags(11)))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User cannot have more than 10 tags");
            assertTagsUnchanged();
        }

        @Test
        @DisplayName("Should reject a null tag")
        void replaceTags_WithNullTag_ShouldThrow() {
            // Given
            List<String> tags = new ArrayList<>();
            tags.add("valid");
            tags.add(null);

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceTags(user, tags))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Tag cannot be blank");
            assertTagsUnchanged();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "#", " ## "})
        @DisplayName("Should reject an empty, blank or only '#' tag")
        void replaceTags_WithBlankTag_ShouldThrow(String tag) {
            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceTags(user, List.of(tag)))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Tag cannot be blank");
            assertTagsUnchanged();
        }

        @Test
        @DisplayName("Should accept 30 characters after normalizing")
        void replaceTags_WithThirtyCharacters_ShouldAccept() {
            // Given: the '#' and the outer spaces don't count
            String tag = "a".repeat(30);

            // When
            userProfileValidationService.replaceTags(user, List.of("  #" + tag.toUpperCase() + "  "));

            // Then
            assertThat(tagNames()).containsExactly(tag);
        }

        @Test
        @DisplayName("Should reject 31 characters after normalizing")
        void replaceTags_WithThirtyOneCharacters_ShouldThrow() {
            // Given: the inner spaces collapse, so it is 31 characters only after normalizing
            String tag = "a".repeat(15) + "     " + "b".repeat(15);

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceTags(user, List.of(tag)))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Tag must be at most 30 characters");
            assertTagsUnchanged();
        }

        @ParameterizedTest
        @ValueSource(strings = {"c++", "a_b", "rock#roll", "emoji 🎸", "a.b"})
        @DisplayName("Should reject characters other than letters, digits, spaces and '-'")
        void replaceTags_WithInvalidCharacters_ShouldThrow(String tag) {
            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceTags(user, List.of(tag)))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(INVALID_CHARACTERS);
            assertTagsUnchanged();
        }

        @Test
        @DisplayName("Should leave the set unchanged when a later tag is invalid")
        void replaceTags_WithErrorAfterValidTags_ShouldNotChangeSet() {
            // Given: the first tags would remove 'música' and add 'nuevo', the last one fails
            List<String> tags = List.of("arte", "nuevo", "c++");

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceTags(user, tags))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(INVALID_CHARACTERS);
            assertTagsUnchanged();
        }
    }

    @Nested
    @DisplayName("Replacement")
    class Replacement {

        @Test
        @DisplayName("Should apply the diff: keep the instances that stay, remove the missing ones and add the new ones")
        void replaceTags_WithDiff_ShouldKeepInstancesAndAddNew() {
            // When
            userProfileValidationService.replaceTags(user, List.of("#Arte", "podcast"));

            // Then
            assertThat(tagNames()).containsExactlyInAnyOrder("arte", "podcast");

            UserTag keptArt = user.getTags().stream()
                    .filter(t -> t.getTag().equals("arte")).findFirst().orElseThrow();
            assertThat(keptArt).isSameAs(art);
            assertThat(keptArt.getId()).isEqualTo(10L);

            UserTag podcast = user.getTags().stream()
                    .filter(t -> t.getTag().equals("podcast")).findFirst().orElseThrow();
            assertThat(podcast.getUser()).isSameAs(user);
            assertThat(podcast.getId()).isNull();
        }

        @Test
        @DisplayName("Should remove every tag with an empty list")
        void replaceTags_WithEmptyList_ShouldClearSet() {
            // When
            userProfileValidationService.replaceTags(user, List.of());

            // Then
            assertThat(user.getTags()).isEmpty();
        }
    }
}
