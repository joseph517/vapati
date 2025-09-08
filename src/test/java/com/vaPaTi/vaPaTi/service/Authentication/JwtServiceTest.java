package com.vaPaTi.vaPaTi.service.Authentication;

import com.vaPaTi.vaPaTi.dtos.UserTokenData;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.service.JwtService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Tests")
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private User testUser;
    private UserInfo testUserInfo;
    private Role testRole;
    private String validToken;
    private String expiredToken;
    private String invalidToken;
    private final String TEST_SECRET = "mySecretKeyForTestingThatIsLongEnoughForHS256Algorithm";
    private final long TEST_EXPIRATION = 86400000L; // 1 día
    private final long TEST_REFRESH_EXPIRATION = 604800000L; // 7 días

    @BeforeEach
    void setUp() {
        // Set properties using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", TEST_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpirationMs", TEST_REFRESH_EXPIRATION);

        // Initialize the service
        jwtService.init();

        // Set up test data
        setupTestData();
        setupTokens();
    }

    private void setupTestData() {
        testRole = Role.builder()
                .id(1L)
                .name("USER")
                .build();

        testUserInfo = UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .userName("johndoe")
                .email("john.doe@example.com")
                .build();

        testUser = User.builder()
                .id(1L)
                .role(testRole)
                .userInfo(testUserInfo)
                .active(true)
                .verified(true)
                .build();

        // Set bidirectional relationship
        testUserInfo.setUser(testUser);
    }

    private void setupTokens() {
        // Valid token
        validToken = jwtService.generateToken(testUser);

        // Expired token - using reflection to create a token with a past date
        try {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            Date pastDate = new Date(System.currentTimeMillis() - 3600000); // 1 hour ago
            expiredToken = Jwts.builder()
                    .setSubject(testUser.getUserInfo().getEmail())
                    .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
                    .setExpiration(pastDate)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            expiredToken = "expired.token.here";
        }

        // Invalid token
        invalidToken = "invalid.jwt.token";
    }

    @Test
    @DisplayName("Should correctly initialize the signing key")
    void shouldInitializeSigningKeyCorrectly() {
        // Given - data configured in setUp()

        // When
        jwtService.init();

        // Then
        Object signingKey = ReflectionTestUtils.getField(jwtService, "signingKey");
        assertThat(signingKey).isNotNull();
    }

    @Test
    @DisplayName("Should generate valid token with all user claims")
    void shouldGenerateValidTokenWithAllUserClaims() {
        // When
        String token = jwtService.generateToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts

        // Verify that the token contains all expected claims
        UserTokenData extractedData = jwtService.extractUserData(token);
        assertThat(extractedData.getUserId()).isEqualTo(testUser.getId());
        assertThat(extractedData.getEmail()).isEqualTo(testUser.getUserInfo().getEmail());
        assertThat(extractedData.getRole()).isEqualTo(testUser.getRole().getName());
        assertThat(extractedData.getFirstName()).isEqualTo(testUser.getUserInfo().getFirstName());
        assertThat(extractedData.getLastName()).isEqualTo(testUser.getUserInfo().getLastName());
        assertThat(extractedData.getUserName()).isEqualTo(testUser.getUserInfo().getUserName());
    }

    @Test
    @DisplayName("Should throw exception when user is null while generating token")
    void shouldThrowExceptionWhenUserIsNullForGenerateToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle user with null info when generating token")
    void shouldHandleUserWithNullInfoWhenGeneratingToken() {
        // Given
        User userWithNullInfo = User.builder()
                .id(1L)
                .role(testRole)
                .userInfo(null)
                .build();

        // When & Then
        assertThatThrownBy(() -> jwtService.generateToken(userWithNullInfo))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should generate valid refresh token with minimal claims")
    void shouldGenerateValidRefreshTokenWithMinimalClaims() {
        // When
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.split("\\.")).hasSize(3);

        // Verify it contains only minimal claims
        String extractedEmail = jwtService.extractUsername(refreshToken);
        assertThat(extractedEmail).isEqualTo(testUser.getUserInfo().getEmail());

        // Refresh token should have a longer expiration
        Date expiration = jwtService.extractExpiration(refreshToken);
        Date normalTokenExpiration = jwtService.extractExpiration(validToken);
        assertThat(expiration).isAfter(normalTokenExpiration);
    }

    @Test
    @DisplayName("Should throw exception when user is null while generating refresh token")
    void shouldThrowExceptionWhenUserIsNullForGenerateRefreshToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.generateRefreshToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should correctly extract username from valid token")
    void shouldExtractUsernameFromValidToken() {
        // When
        String extractedUsername = jwtService.extractUsername(validToken);

        // Then
        assertThat(extractedUsername).isEqualTo(testUser.getUserInfo().getEmail());
    }

    @Test
    @DisplayName("Should throw exception when extracting username from invalid token")
    void shouldThrowExceptionWhenExtractingUsernameFromInvalidToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should correctly extract expiration date")
    void shouldExtractExpirationDateCorrectly() {
        // When
        Date expiration = jwtService.extractExpiration(validToken);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should throw exception when extracting expiration from invalid token")
    void shouldThrowExceptionWhenExtractingExpirationFromInvalidToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.extractExpiration(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should correctly extract all user data")
    void shouldExtractAllUserDataCorrectly() {
        // When
        UserTokenData userData = jwtService.extractUserData(validToken);

        // Then
        assertThat(userData).isNotNull();
        assertThat(userData.getUserId()).isEqualTo(testUser.getId());
        assertThat(userData.getEmail()).isEqualTo(testUser.getUserInfo().getEmail());
        assertThat(userData.getRole()).isEqualTo(testUser.getRole().getName());
        assertThat(userData.getFirstName()).isEqualTo(testUser.getUserInfo().getFirstName());
        assertThat(userData.getLastName()).isEqualTo(testUser.getUserInfo().getLastName());
        assertThat(userData.getUserName()).isEqualTo(testUser.getUserInfo().getUserName());
        assertThat(userData.getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should handle null claims when extracting user data")
    void shouldHandleNullClaimsWhenExtractingUserData() {
        // Given - create token with partial claims
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        String tokenWithPartialClaims = Jwts.builder()
                .setSubject(testUser.getUserInfo().getEmail())
                .claim("userId", testUser.getId())
                // Intentionally omit some claims
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // When
        UserTokenData userData = jwtService.extractUserData(tokenWithPartialClaims);

        // Then
        assertThat(userData).isNotNull();
        assertThat(userData.getUserId()).isEqualTo(testUser.getId());
        assertThat(userData.getEmail()).isEqualTo(testUser.getUserInfo().getEmail());
        assertThat(userData.getRole()).isNull();
        assertThat(userData.getFirstName()).isNull();
        assertThat(userData.getLastName()).isNull();
        assertThat(userData.getUserName()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when extracting user data from invalid token")
    void shouldThrowExceptionWhenExtractingUserDataFromInvalidToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.extractUserData(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should extract specific claim using function resolver")
    void shouldExtractSpecificClaimUsingFunctionResolver() {
        // Given
        Function<Claims, String> subjectResolver = Claims::getSubject;

        // When
        String subject = jwtService.extractClaim(validToken, subjectResolver);

        // Then
        assertThat(subject).isEqualTo(testUser.getUserInfo().getEmail());
    }

    @Test
    @DisplayName("Should extract custom claim using function resolver")
    void shouldExtractCustomClaimUsingFunctionResolver() {
        // Given
        Function<Claims, Long> userIdResolver = claims -> claims.get("userId", Long.class);

        // When
        Long userId = jwtService.extractClaim(validToken, userIdResolver);

        // Then
        assertThat(userId).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should throw exception when claims resolver is null")
    void shouldThrowExceptionWhenClaimsResolverIsNull() {
        // When & Then
        assertThatThrownBy(() -> jwtService.extractClaim(validToken, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception when extracting claim from invalid token")
    void shouldThrowExceptionWhenExtractingClaimFromInvalidToken() {
        // Given
        Function<Claims, String> subjectResolver = Claims::getSubject;

        // When & Then
        assertThatThrownBy(() -> jwtService.extractClaim(invalidToken, subjectResolver))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should validate token correctly with matching email")
    void shouldValidateTokenWithMatchingEmail() {
        // When
        boolean isValid = jwtService.isTokenValid(validToken, testUser.getUserInfo().getEmail());

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token with non-matching email")
    void shouldRejectTokenWithNonMatchingEmail() {
        // When
        boolean isValid = jwtService.isTokenValid(validToken, "different@example.com");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // When
        boolean isValid = jwtService.isTokenValid(expiredToken, testUser.getUserInfo().getEmail());

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for an invalid token")
    void shouldReturnFalseForInvalidToken() {
        // Token mal formado que no puede ser parseado
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalid-payload.invalid-signature";

        // When
        boolean isValid = jwtService.isTokenValid(invalidToken, testUser.getUserInfo().getEmail());

        // Then
        assertThat(isValid).isFalse();
    }


    @Test
    @DisplayName("Should handle null token during validation")
    void shouldHandleNullTokenDuringValidation() {
        // When & Then
        assertThatThrownBy(() -> jwtService.isTokenValid(null, testUser.getUserInfo().getEmail()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle null email during validation")
    void shouldHandleNullEmailDuringValidation() {
        // When
        boolean isValid = jwtService.isTokenValid(validToken, null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle invalid signature exception")
    void shouldHandleInvalidSignatureException() {
        // Given - token signed with different key
        SecretKey differentKey = Keys.hmacShaKeyFor("differentSecretKeyForTestingPurposes123".getBytes());
        String tokenWithDifferentSignature = Jwts.builder()
                .setSubject(testUser.getUserInfo().getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(differentKey, SignatureAlgorithm.HS256)
                .compact();

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(tokenWithDifferentSignature))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Should handle malformed token")
    void shouldHandleMalformedToken() {
        // Given
        String malformedToken = "this.is.malformed";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Should handle token without claims")
    void shouldHandleTokenWithoutClaims() {
        // Given
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        String tokenWithoutClaims = Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // When
        String username = jwtService.extractUsername(tokenWithoutClaims);

        // Then
        assertThat(username).isNull();
    }

    @Test
    @DisplayName("Should generate different tokens for the same user")
    void shouldGenerateDifferentTokensForSameUser() {
        // When
        String token1 = jwtService.generateToken(testUser);
        String token2 = jwtService.generateToken(testUser);

        // Then
        assertThat(token1).isNotEqualTo(token2);

        // But both should be valid for the same user
        assertThat(jwtService.isTokenValid(token1, testUser.getUserInfo().getEmail())).isTrue();
        assertThat(jwtService.isTokenValid(token2, testUser.getUserInfo().getEmail())).isTrue();
    }

    @Test
    @DisplayName("Should generate refresh and normal token with different expirations")
    void shouldGenerateRefreshTokenAndNormalTokenWithDifferentExpirations() {
        // When
        String normalToken = jwtService.generateToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // Then
        Date normalExpiration = jwtService.extractExpiration(normalToken);
        Date refreshExpiration = jwtService.extractExpiration(refreshToken);

        assertThat(refreshExpiration).isAfter(normalExpiration);

        // Check that the difference is approximately as expected
        long timeDifference = refreshExpiration.getTime() - normalExpiration.getTime();
        long expectedDifference = TEST_REFRESH_EXPIRATION - TEST_EXPIRATION;
        assertThat(Math.abs(timeDifference - expectedDifference)).isLessThan(1000); // 1-second margin
    }

}
