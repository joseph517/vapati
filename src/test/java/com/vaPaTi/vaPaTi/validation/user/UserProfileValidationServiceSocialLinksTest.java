package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.dtos.user.SocialLinkDTO;
import com.vaPaTi.vaPaTi.entity.user.SocialPlatform;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserSocialLink;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileValidationService - Social Links Tests")
class UserProfileValidationServiceSocialLinksTest {

    private static final String INSTAGRAM_URL = "https://instagram.com/old";
    private static final String X_URL = "https://x.com/old";

    @Mock
    private TextValidationService textValidationService;

    // Real instance: the URL rule lives in UrlValidationService
    @Spy
    private UrlValidationService urlValidationService = new UrlValidationService();

    @InjectMocks
    private UserProfileValidationService userProfileValidationService;

    private User user;
    private UserSocialLink instagram;
    private UserSocialLink x;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).build();
        instagram = UserSocialLink.builder().id(10L).user(user).platform(SocialPlatform.INSTAGRAM).url(INSTAGRAM_URL).build();
        x = UserSocialLink.builder().id(11L).user(user).platform(SocialPlatform.X).url(X_URL).build();
        user.setSocialLinks(new HashSet<>(Set.of(instagram, x)));
    }

    private static SocialLinkDTO link(SocialPlatform platform, String url) {
        return SocialLinkDTO.builder().platform(platform).url(url).build();
    }

    private void assertLinksUnchanged() {
        assertThat(user.getSocialLinks())
                .extracting(UserSocialLink::getPlatform, UserSocialLink::getUrl)
                .containsExactlyInAnyOrder(
                        tuple(SocialPlatform.INSTAGRAM, INSTAGRAM_URL),
                        tuple(SocialPlatform.X, X_URL));
    }

    @Nested
    @DisplayName("Validation errors")
    class ValidationErrors {

        @Test
        @DisplayName("Should reject more than 8 links before checking the elements")
        void replaceSocialLinks_WithNineLinks_ShouldThrowLimitError() {
            // Given: 9 elements, all null, so an element check would give another message
            List<SocialLinkDTO> links = Arrays.asList(new SocialLinkDTO[9]);

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User cannot have more than 8 social links");
            verify(urlValidationService, never()).normalizeOptionalUrl(anyString(), anyString(), anyInt());
            assertLinksUnchanged();
        }

        @Test
        @DisplayName("Should accept exactly 8 links")
        void replaceSocialLinks_WithEightLinks_ShouldReplace() {
            // Given
            List<SocialLinkDTO> links = List.of(
                    link(SocialPlatform.INSTAGRAM, "https://instagram.com/a"),
                    link(SocialPlatform.X, "https://x.com/a"),
                    link(SocialPlatform.TIKTOK, "https://www.tiktok.com/@a"),
                    link(SocialPlatform.YOUTUBE, "https://youtu.be/a"),
                    link(SocialPlatform.TWITCH, "https://twitch.tv/a"),
                    link(SocialPlatform.FACEBOOK, "https://m.facebook.com/a"),
                    link(SocialPlatform.LINKEDIN, "https://www.linkedin.com/in/a"),
                    link(SocialPlatform.GITHUB, "https://github.com/a"));

            // When
            userProfileValidationService.replaceSocialLinks(user, links);

            // Then
            assertThat(user.getSocialLinks()).hasSize(8);
        }

        @Test
        @DisplayName("Should reject a null element")
        void replaceSocialLinks_WithNullElement_ShouldThrow() {
            // Given
            List<SocialLinkDTO> links = new ArrayList<>();
            links.add(link(SocialPlatform.GITHUB, "https://github.com/a"));
            links.add(null);

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Social link platform is required");
            assertLinksUnchanged();
        }

        @Test
        @DisplayName("Should reject a link without platform")
        void replaceSocialLinks_WithNullPlatform_ShouldThrow() {
            // Given
            List<SocialLinkDTO> links = List.of(link(null, "https://github.com/a"));

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Social link platform is required");
            assertLinksUnchanged();
        }

        @Test
        @DisplayName("Should reject a repeated platform")
        void replaceSocialLinks_WithDuplicatePlatform_ShouldThrow() {
            // Given
            List<SocialLinkDTO> links = List.of(
                    link(SocialPlatform.X, "https://x.com/a"),
                    link(SocialPlatform.X, "https://twitter.com/b"));

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Duplicate social link platform: X");
            assertLinksUnchanged();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should reject a blank URL")
        void replaceSocialLinks_WithBlankUrl_ShouldThrow(String url) {
            // Given
            List<SocialLinkDTO> links = List.of(link(SocialPlatform.INSTAGRAM, url));

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("INSTAGRAM link URL is required");
            assertLinksUnchanged();
        }

        @Test
        @DisplayName("Should reject a null URL")
        void replaceSocialLinks_WithNullUrl_ShouldThrow() {
            // Given
            List<SocialLinkDTO> links = List.of(link(SocialPlatform.INSTAGRAM, null));

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("INSTAGRAM link URL is required");
            assertLinksUnchanged();
        }

        @ParameterizedTest
        @ValueSource(strings = {"ftp://instagram.com/a", "instagram.com/a", "javascript:alert(1)", "https://"})
        @DisplayName("Should reject a URL that is not http or https with a host")
        void replaceSocialLinks_WithInvalidUrl_ShouldThrow(String url) {
            // Given
            List<SocialLinkDTO> links = List.of(link(SocialPlatform.INSTAGRAM, url));

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("INSTAGRAM link must be a valid http or https URL");
            assertLinksUnchanged();
        }

        @ParameterizedTest
        @ValueSource(strings = {"https://evil.com/a", "https://instagram.com.evil.com/a",
                "https://evilinstagram.com/a", "https://instagram.com@evil.com/a"})
        @DisplayName("Should reject a host that is not the platform's domain")
        void replaceSocialLinks_WithForeignHost_ShouldThrow(String url) {
            // Given
            List<SocialLinkDTO> links = List.of(link(SocialPlatform.INSTAGRAM, url));

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("INSTAGRAM link must point to instagram.com");
            assertLinksUnchanged();
        }

        @Test
        @DisplayName("Should list every domain of the platform in the host error")
        void replaceSocialLinks_WithForeignHostOnMultiDomainPlatform_ShouldListDomains() {
            // Given
            List<SocialLinkDTO> links = List.of(link(SocialPlatform.X, "https://mastodon.social/@a"));

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("X link must point to x.com or twitter.com");
            assertLinksUnchanged();
        }

        @Test
        @DisplayName("Should leave the set unchanged when a later element is invalid")
        void replaceSocialLinks_WithErrorAfterValidElements_ShouldNotChangeSet() {
            // Given: the first elements would change X and add GITHUB, the last one fails
            List<SocialLinkDTO> links = List.of(
                    link(SocialPlatform.X, "https://x.com/new"),
                    link(SocialPlatform.GITHUB, "https://github.com/a"),
                    link(SocialPlatform.LINKEDIN, "https://evil.com/a"));

            // When & Then
            assertThatThrownBy(() -> userProfileValidationService.replaceSocialLinks(user, links))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("LINKEDIN link must point to linkedin.com");
            assertLinksUnchanged();
        }
    }

    @Nested
    @DisplayName("Replacement")
    class Replacement {

        @Test
        @DisplayName("Should accept any http or https host for OTHER")
        void replaceSocialLinks_WithOtherPlatform_ShouldAcceptAnyHost() {
            // Given
            List<SocialLinkDTO> links = List.of(link(SocialPlatform.OTHER, "https://my-blog.example.org/about"));

            // When
            userProfileValidationService.replaceSocialLinks(user, links);

            // Then
            assertThat(user.getSocialLinks())
                    .extracting(UserSocialLink::getPlatform, UserSocialLink::getUrl)
                    .containsExactly(tuple(SocialPlatform.OTHER, "https://my-blog.example.org/about"));
        }

        @Test
        @DisplayName("Should accept a subdomain and a host in uppercase, and store the URL trimmed")
        void replaceSocialLinks_WithSubdomainAndSpaces_ShouldStoreTrimmed() {
            // Given
            List<SocialLinkDTO> links = List.of(link(SocialPlatform.INSTAGRAM, "  https://WWW.Instagram.com/new  "));

            // When
            userProfileValidationService.replaceSocialLinks(user, links);

            // Then
            assertThat(user.getSocialLinks())
                    .extracting(UserSocialLink::getPlatform, UserSocialLink::getUrl)
                    .containsExactly(tuple(SocialPlatform.INSTAGRAM, "https://WWW.Instagram.com/new"));
        }

        @Test
        @DisplayName("Should apply the diff: remove missing platforms, update in place and add new ones")
        void replaceSocialLinks_WithDiff_ShouldKeepInstancesAndAddNew() {
            // Given: current {INSTAGRAM, X}, request {X with another url, GITHUB}
            List<SocialLinkDTO> links = List.of(
                    link(SocialPlatform.X, "https://twitter.com/new"),
                    link(SocialPlatform.GITHUB, "https://github.com/new"));

            // When
            userProfileValidationService.replaceSocialLinks(user, links);

            // Then
            assertThat(user.getSocialLinks())
                    .extracting(UserSocialLink::getPlatform, UserSocialLink::getUrl)
                    .containsExactlyInAnyOrder(
                            tuple(SocialPlatform.X, "https://twitter.com/new"),
                            tuple(SocialPlatform.GITHUB, "https://github.com/new"));

            UserSocialLink keptX = user.getSocialLinks().stream()
                    .filter(l -> l.getPlatform() == SocialPlatform.X).findFirst().orElseThrow();
            assertThat(keptX).isSameAs(x);
            assertThat(keptX.getId()).isEqualTo(11L);

            UserSocialLink github = user.getSocialLinks().stream()
                    .filter(l -> l.getPlatform() == SocialPlatform.GITHUB).findFirst().orElseThrow();
            assertThat(github.getUser()).isSameAs(user);
            assertThat(github.getId()).isNull();
        }

        @Test
        @DisplayName("Should remove every link with an empty list")
        void replaceSocialLinks_WithEmptyList_ShouldClearSet() {
            // When
            userProfileValidationService.replaceSocialLinks(user, List.of());

            // Then
            assertThat(user.getSocialLinks()).isEmpty();
        }

        @Test
        @DisplayName("Should add links to a user without links")
        void replaceSocialLinks_WhenUserHasNoLinks_ShouldAdd() {
            // Given
            user.setSocialLinks(new HashSet<>());
            List<SocialLinkDTO> links = List.of(link(SocialPlatform.TWITCH, "https://www.twitch.tv/a"));

            // When
            userProfileValidationService.replaceSocialLinks(user, links);

            // Then
            assertThat(user.getSocialLinks())
                    .extracting(UserSocialLink::getPlatform, UserSocialLink::getUrl, UserSocialLink::getUser)
                    .containsExactly(tuple(SocialPlatform.TWITCH, "https://www.twitch.tv/a", user));
        }
    }
}
