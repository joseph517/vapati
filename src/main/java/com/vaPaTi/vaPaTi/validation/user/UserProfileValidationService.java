package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.dtos.user.UpdateUserDTO;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.common.UrlValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

// Public profile fields: the default thanks message sent to supporters and the profile URLs
@Service
@RequiredArgsConstructor
public class UserProfileValidationService {

    static final int MAX_URL_LENGTH = 255;
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

    private String validateAndNormalizeIntroVideoUrl(String introVideoUrl) {
        String url = urlValidationService.normalizeOptionalUrl(introVideoUrl, "Intro video", MAX_URL_LENGTH);
        if (url != null && !VIDEO_HOSTS.contains(URI.create(url).getHost().toLowerCase(Locale.ROOT))) {
            throw new MessageException("Intro video must be a YouTube or Vimeo URL");
        }
        return url;
    }
}
