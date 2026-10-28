package com.vaPaTi.vaPaTi.entity.user;

import java.util.List;
import java.util.Locale;

// Each platform holds the domains its link may point to; a subdomain (www., m.) matches too. OTHER accepts any host.
// The declaration order is the order of the links in the responses
public enum SocialPlatform {
    INSTAGRAM("instagram.com"),
    X("x.com", "twitter.com"),
    TIKTOK("tiktok.com"),
    YOUTUBE("youtube.com", "youtu.be"),
    TWITCH("twitch.tv"),
    FACEBOOK("facebook.com", "fb.com"),
    LINKEDIN("linkedin.com"),
    GITHUB("github.com"),
    OTHER();

    private final List<String> domains;

    SocialPlatform(String... domains) {
        this.domains = List.of(domains);
    }

    public List<String> getDomains() {
        return domains;
    }

    public boolean acceptsHost(String host) {
        if (domains.isEmpty()) {
            return true;
        }
        String h = host.toLowerCase(Locale.ROOT);
        return domains.stream().anyMatch(d -> h.equals(d) || h.endsWith("." + d));
    }
}
