package com.vaPaTi.vaPaTi.entity.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SocialPlatform - Unit Tests")
class SocialPlatformTest {

    @ParameterizedTest
    @ValueSource(strings = {"instagram.com", "www.instagram.com", "WWW.Instagram.com"})
    @DisplayName("Should accept the platform domain and its subdomains, ignoring case")
    void acceptsHost_WithOwnDomainOrSubdomain_ShouldReturnTrue(String host) {
        // When & Then
        assertThat(SocialPlatform.INSTAGRAM.acceptsHost(host)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"instagram.com.evil.com", "evilinstagram.com", "evil.com"})
    @DisplayName("Should reject hosts that only contain the platform domain")
    void acceptsHost_WithLookalikeHost_ShouldReturnFalse(String host) {
        // When & Then
        assertThat(SocialPlatform.INSTAGRAM.acceptsHost(host)).isFalse();
    }

    @Test
    @DisplayName("Should accept every domain of a platform with several domains")
    void acceptsHost_WithEachDomainOfPlatform_ShouldReturnTrue() {
        // When & Then
        assertThat(SocialPlatform.X.acceptsHost("x.com")).isTrue();
        assertThat(SocialPlatform.X.acceptsHost("twitter.com")).isTrue();
        assertThat(SocialPlatform.YOUTUBE.acceptsHost("youtu.be")).isTrue();
        assertThat(SocialPlatform.YOUTUBE.acceptsHost("youtube.com")).isTrue();
    }

    @Test
    @DisplayName("Should reject the domain of another platform")
    void acceptsHost_WithOtherPlatformDomain_ShouldReturnFalse() {
        // When & Then
        assertThat(SocialPlatform.X.acceptsHost("instagram.com")).isFalse();
        assertThat(SocialPlatform.FACEBOOK.acceptsHost("m.facebook.com")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com", "instagram.com", "my-site.dev"})
    @DisplayName("OTHER should accept any host and have no domains")
    void acceptsHost_OnOther_ShouldAcceptAnyHost(String host) {
        // When & Then
        assertThat(SocialPlatform.OTHER.acceptsHost(host)).isTrue();
        assertThat(SocialPlatform.OTHER.getDomains()).isEmpty();
    }
}
