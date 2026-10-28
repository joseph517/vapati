package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.dtos.user.UpdateUserDTO;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileValidationService - Profile URL Fields Tests")
class UserProfileValidationServiceFieldsTest {

    private static final String PICTURE = "https://cdn.example.com/old-picture.png";
    private static final String COVER = "https://cdn.example.com/old-cover.png";
    private static final String VIDEO = "https://www.youtube.com/watch?v=old";
    private static final String WEBSITE = "https://old.example.com";

    @Mock
    private TextValidationService textValidationService;

    // Real instance: the URL rule lives in UrlValidationService
    @Spy
    private UrlValidationService urlValidationService = new UrlValidationService();

    @InjectMocks
    private UserProfileValidationService userProfileValidationService;

    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        userInfo = UserInfo.builder()
                .profilePicture(PICTURE)
                .coverImageUrl(COVER)
                .introVideoUrl(VIDEO)
                .website(WEBSITE)
                .build();
    }

    @Nested
    @DisplayName("validateAndNormalizeProfilePicture")
    class ProfilePicture {

        @ParameterizedTest
        @ValueSource(strings = {"", "  "})
        @DisplayName("Should return null when the profile picture is blank")
        void validateAndNormalizeProfilePicture_WhenBlank_ShouldReturnNull(String value) {
            // When & Then
            assertThat(userProfileValidationService.validateAndNormalizeProfilePicture(value)).isNull();
        }

        @Test
        @DisplayName("Should return null when the profile picture is null")
        void validateAndNormalizeProfilePicture_WhenNull_ShouldReturnNull() {
            // When & Then
            assertThat(userProfileValidationService.validateAndNormalizeProfilePicture(null)).isNull();
        }

        @Test
        @DisplayName("Should return the URL trimmed")
        void validateAndNormalizeProfilePicture_WithSurroundingSpaces_ShouldReturnTrimmed() {
            // When
            String result = userProfileValidationService.validateAndNormalizeProfilePicture("  https://cdn.example.com/p.png  ");

            // Then
            assertThat(result).isEqualTo("https://cdn.example.com/p.png");
        }

        @Test
        @DisplayName("Should throw when the profile picture is not an http or https URL")
        void validateAndNormalizeProfilePicture_WhenNotUrl_ShouldThrow() {
            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.validateAndNormalizeProfilePicture("profile.jpg"))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Profile picture must be a valid http or https URL");
        }
    }

    @Nested
    @DisplayName("applyProfileFields")
    class ApplyProfileFields {

        @Test
        @DisplayName("Should keep every field when none is sent")
        void applyProfileFields_WithAllNull_ShouldChangeNothing() {
            // When
            userProfileValidationService.applyProfileFields(userInfo, new UpdateUserDTO());

            // Then
            assertThat(userInfo.getProfilePicture()).isEqualTo(PICTURE);
            assertThat(userInfo.getCoverImageUrl()).isEqualTo(COVER);
            assertThat(userInfo.getIntroVideoUrl()).isEqualTo(VIDEO);
            assertThat(userInfo.getWebsite()).isEqualTo(WEBSITE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  "})
        @DisplayName("Should clear every field sent blank")
        void applyProfileFields_WithBlankValues_ShouldClearFields(String blank) {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder()
                    .profilePicture(blank).coverImageUrl(blank).introVideoUrl(blank).website(blank).build();

            // When
            userProfileValidationService.applyProfileFields(userInfo, dto);

            // Then
            assertThat(userInfo.getProfilePicture()).isNull();
            assertThat(userInfo.getCoverImageUrl()).isNull();
            assertThat(userInfo.getIntroVideoUrl()).isNull();
            assertThat(userInfo.getWebsite()).isNull();
        }

        @Test
        @DisplayName("Should store every valid URL trimmed")
        void applyProfileFields_WithValidUrlsAndSpaces_ShouldStoreTrimmed() {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder()
                    .profilePicture("  https://cdn.example.com/new.png ")
                    .coverImageUrl(" https://cdn.example.com/cover.png  ")
                    .introVideoUrl("  https://youtu.be/abc  ")
                    .website(" https://my-site.example.com ")
                    .build();

            // When
            userProfileValidationService.applyProfileFields(userInfo, dto);

            // Then
            assertThat(userInfo.getProfilePicture()).isEqualTo("https://cdn.example.com/new.png");
            assertThat(userInfo.getCoverImageUrl()).isEqualTo("https://cdn.example.com/cover.png");
            assertThat(userInfo.getIntroVideoUrl()).isEqualTo("https://youtu.be/abc");
            assertThat(userInfo.getWebsite()).isEqualTo("https://my-site.example.com");
        }

        @Test
        @DisplayName("Should change only the field sent")
        void applyProfileFields_WithOneField_ShouldChangeOnlyThatField() {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder().website("https://new.example.com").build();

            // When
            userProfileValidationService.applyProfileFields(userInfo, dto);

            // Then
            assertThat(userInfo.getWebsite()).isEqualTo("https://new.example.com");
            assertThat(userInfo.getProfilePicture()).isEqualTo(PICTURE);
            assertThat(userInfo.getCoverImageUrl()).isEqualTo(COVER);
            assertThat(userInfo.getIntroVideoUrl()).isEqualTo(VIDEO);
        }

        @Test
        @DisplayName("Should throw the profile picture message when it is not a URL")
        void applyProfileFields_WithInvalidProfilePicture_ShouldThrow() {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder().profilePicture("profile.jpg").build();

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.applyProfileFields(userInfo, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Profile picture must be a valid http or https URL");
            assertThat(userInfo.getProfilePicture()).isEqualTo(PICTURE);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "https://youtu.be/abc",
                "https://www.youtube.com/watch?v=abc",
                "https://m.youtube.com/watch?v=abc",
                "https://vimeo.com/123",
                "https://YouTube.com/watch?v=abc"
        })
        @DisplayName("Should accept a YouTube or Vimeo intro video")
        void applyProfileFields_WithVideoHost_ShouldStoreIntroVideo(String url) {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder().introVideoUrl(url).build();

            // When
            userProfileValidationService.applyProfileFields(userInfo, dto);

            // Then
            assertThat(userInfo.getIntroVideoUrl()).isEqualTo(url);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "https://dailymotion.com/video/x",
                "https://youtube.com.evil.com/watch?v=abc",
                "https://player.vimeo.com/video/123"
        })
        @DisplayName("Should throw when the intro video is not on a YouTube or Vimeo host")
        void applyProfileFields_WithOtherVideoHost_ShouldThrow(String url) {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder().introVideoUrl(url).build();

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.applyProfileFields(userInfo, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Intro video must be a YouTube or Vimeo URL");
            assertThat(userInfo.getIntroVideoUrl()).isEqualTo(VIDEO);
        }

        @Test
        @DisplayName("Should throw the intro video message when it is not a URL")
        void applyProfileFields_WithInvalidIntroVideo_ShouldThrow() {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder().introVideoUrl("youtube.com/watch?v=abc").build();

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.applyProfileFields(userInfo, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Intro video must be a valid http or https URL");
        }

        @Test
        @DisplayName("Should throw the website message when it is not http or https")
        void applyProfileFields_WithFtpWebsite_ShouldThrow() {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder().website("ftp://x.com").build();

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.applyProfileFields(userInfo, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Website must be a valid http or https URL");
            assertThat(userInfo.getWebsite()).isEqualTo(WEBSITE);
        }

        @Test
        @DisplayName("Should throw the cover image message when it is not a URL")
        void applyProfileFields_WithInvalidCoverImage_ShouldThrow() {
            // Given
            UpdateUserDTO dto = UpdateUserDTO.builder().coverImageUrl("javascript:alert(1)").build();

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.applyProfileFields(userInfo, dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Cover image must be a valid http or https URL");
            assertThat(userInfo.getCoverImageUrl()).isEqualTo(COVER);
        }
    }
}
