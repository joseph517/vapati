package com.vaPaTi.vaPaTi.security;

import com.vaPaTi.vaPaTi.dtos.UserTokenData;
import com.vaPaTi.vaPaTi.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticatedUserService - Unit Tests")
class AuthenticatedUserServiceTest {

    private static final String TOKEN = "valid.jwt.token";

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticatedUserService authenticatedUserService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserTokenData tokenDataFor(Long userId) {
        return UserTokenData.builder().userId(userId).email("john.doe@example.com").build();
    }

    private void givenAuthenticatedWithToken(Object credentials) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john.doe@example.com", credentials, List.of()));
    }

    @Nested
    @DisplayName("findAuthenticatedUserId Tests")
    class FindAuthenticatedUserIdTests {

        @Test
        @DisplayName("Should return empty when there is no Authentication")
        void findAuthenticatedUserId_WhenNoAuthentication_ShouldReturnEmpty() {
            assertThat(authenticatedUserService.findAuthenticatedUserId()).isEmpty();

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Should return empty when the Authentication is anonymous")
        void findAuthenticatedUserId_WhenAnonymousAuthentication_ShouldReturnEmpty() {
            // Given: what Spring sets on a permitAll route called without a JWT
            SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                    "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

            assertThat(authenticatedUserService.findAuthenticatedUserId()).isEmpty();

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Should return empty when the credentials are not a token")
        void findAuthenticatedUserId_WhenCredentialsAreNotAString_ShouldReturnEmpty() {
            givenAuthenticatedWithToken(null);

            assertThat(authenticatedUserService.findAuthenticatedUserId()).isEmpty();

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Should return the user id when the token is valid")
        void findAuthenticatedUserId_WhenValidToken_ShouldReturnUserId() {
            // Given
            givenAuthenticatedWithToken(TOKEN);
            when(jwtService.extractUserData(TOKEN)).thenReturn(tokenDataFor(7L));

            // When & Then
            assertThat(authenticatedUserService.findAuthenticatedUserId()).contains(7L);
        }
    }

    @Nested
    @DisplayName("getAuthenticatedUserId Tests")
    class GetAuthenticatedUserIdTests {

        @Test
        @DisplayName("Should return the user id when the token is valid")
        void getAuthenticatedUserId_WhenValidToken_ShouldReturnUserId() {
            // Given
            givenAuthenticatedWithToken(TOKEN);
            when(jwtService.extractUserData(TOKEN)).thenReturn(tokenDataFor(7L));

            // When & Then
            assertThat(authenticatedUserService.getAuthenticatedUserId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when there is no Authentication")
        void getAuthenticatedUserId_WhenNoAuthentication_ShouldThrowIllegalStateException() {
            assertThatThrownBy(() -> authenticatedUserService.getAuthenticatedUserId())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot extract token from SecurityContext");
        }
    }
}
