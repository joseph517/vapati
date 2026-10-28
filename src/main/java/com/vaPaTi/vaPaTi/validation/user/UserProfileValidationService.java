package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.dtos.user.SocialLinkDTO;
import com.vaPaTi.vaPaTi.dtos.user.UpdateUserDTO;
import com.vaPaTi.vaPaTi.entity.user.SocialPlatform;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.entity.user.UserSocialLink;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.common.UrlValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Public profile fields: the default thanks message sent to supporters, the profile URLs and the social links
@Service
@RequiredArgsConstructor
public class UserProfileValidationService {

    static final int MAX_URL_LENGTH = 255;
    static final int MAX_SOCIAL_LINKS = 8;
    private static final int MAX_AUTO_THANKS_LENGTH = 280;
    // Exact list, no subdomains: the front embeds the video
    private static final Set<String> VIDEO_HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be", "vimeo.com", "www.vimeo.com");
    private final TextValidationService textValidationService;
    private final UrlValidationService urlValidationService;

    // The length check repeats the DTO's @Size, as validateAndNormalizeMessage does for donations
    public String validateAndNormalizeAutoThanksMessage(String autoThanksMessage) {
        return textValidationService.normalizeRequiredText(autoThanksMessage, "Auto thanks message", MAX_AUTO_THANKS_LENGTH);
    }

    // Blank -> null. Used by the user creation and by applyProfileFields
    public String validateAndNormalizeProfilePicture(String profilePicture) {
        return urlValidationService.normalizeOptionalUrl(profilePicture, "Profile picture", MAX_URL_LENGTH);
    }

    // Only the fields sent (non-null) change; a blank value clears the field. The timestamp is set by updateUserInfo
    public void applyProfileFields(UserInfo userInfo, UpdateUserDTO dto) {
        if (dto.getProfilePicture() != null) {
            userInfo.setProfilePicture(validateAndNormalizeProfilePicture(dto.getProfilePicture()));
        }
        if (dto.getCoverImageUrl() != null) {
            userInfo.setCoverImageUrl(
                    urlValidationService.normalizeOptionalUrl(dto.getCoverImageUrl(), "Cover image", MAX_URL_LENGTH));
        }
        if (dto.getIntroVideoUrl() != null) {
            userInfo.setIntroVideoUrl(validateAndNormalizeIntroVideoUrl(dto.getIntroVideoUrl()));
        }
        if (dto.getWebsite() != null) {
            userInfo.setWebsite(urlValidationService.normalizeOptionalUrl(dto.getWebsite(), "Website", MAX_URL_LENGTH));
        }
    }

    // Validates every link before touching the user's set, so an error leaves the current links as they are
    public void replaceSocialLinks(User user, List<SocialLinkDTO> links) {
        if (links.size() > MAX_SOCIAL_LINKS) {
            throw new MessageException("User cannot have more than " + MAX_SOCIAL_LINKS + " social links");
        }

        Map<SocialPlatform, String> requested = new EnumMap<>(SocialPlatform.class);
        for (SocialLinkDTO link : links) {
            if (link == null || link.getPlatform() == null) {
                throw new MessageException("Social link platform is required");
            }
            SocialPlatform platform = link.getPlatform();
            if (requested.containsKey(platform)) {
                throw new MessageException("Duplicate social link platform: " + platform);
            }
            requested.put(platform, validateAndNormalizeSocialLinkUrl(platform, link.getUrl()));
        }

        // Diff, not clear()+add(): Hibernate flushes INSERTs before orphan DELETEs, so re-adding a platform would hit UNIQUE(user_id, platform)
        Set<UserSocialLink> current = user.getSocialLinks();
        current.removeIf(l -> !requested.containsKey(l.getPlatform()));
        Set<SocialPlatform> existingPlatforms = EnumSet.noneOf(SocialPlatform.class);
        for (UserSocialLink existing : current) {
            existing.setUrl(requested.get(existing.getPlatform()));
            existingPlatforms.add(existing.getPlatform());
        }
        requested.forEach((platform, url) -> {
            if (!existingPlatforms.contains(platform)) {
                current.add(UserSocialLink.builder().user(user).platform(platform).url(url).build());
            }
        });
    }

    private String validateAndNormalizeSocialLinkUrl(SocialPlatform platform, String url) {
        if (url == null || url.isBlank()) {
            throw new MessageException(platform + " link URL is required");
        }
        String normalized = urlValidationService.normalizeOptionalUrl(url, platform + " link", MAX_URL_LENGTH);
        // normalizeOptionalUrl already checked that the URL parses and has a host, so getHost() is not null here
        if (!platform.acceptsHost(URI.create(normalized).getHost())) {
            throw new MessageException(platform + " link must point to " + String.join(" or ", platform.getDomains()));
        }
        return normalized;
    }

    private String validateAndNormalizeIntroVideoUrl(String introVideoUrl) {
        String url = urlValidationService.normalizeOptionalUrl(introVideoUrl, "Intro video", MAX_URL_LENGTH);
        if (url != null && !VIDEO_HOSTS.contains(URI.create(url).getHost().toLowerCase(Locale.ROOT))) {
            throw new MessageException("Intro video must be a YouTube or Vimeo URL");
        }
        return url;
    }
}
