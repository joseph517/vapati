package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.UserTokenData;
import com.vaPaTi.vaPaTi.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "userId";
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String PLACEHOLDER_SECRET = "your-512-bit-secret-key-should-be-long-and-random";
    private static final int MIN_SECRET_BYTES = 32;

    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    private Key signingKey;

    @PostConstruct
    public void init() {
        validateSecret(jwtSecret);
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Fails startup in every environment with an insecure secret. Never prints the secret itself.
    private static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is empty. Set JWT_SECRET (at least " + MIN_SECRET_BYTES + " bytes)");
        }
        if (PLACEHOLDER_SECRET.equals(secret)) {
            throw new IllegalStateException("JWT secret is the public placeholder. Set JWT_SECRET to a random value, e.g. openssl rand -base64 64 | tr -d '\\n'");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret is too short. JWT_SECRET needs at least " + MIN_SECRET_BYTES + " bytes");
        }
    }

    public String generateToken( User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        Map<String, Object> claims = new HashMap<>();
        // Add user data to the token
        claims.put("role", user.getRole().getName());
        claims.put(USER_ID_CLAIM, user.getId());
        claims.put("firstName", user.getUserInfo().getFirstName());
        claims.put("lastName", user.getUserInfo().getLastName());
        claims.put("userName", user.getUserInfo().getUserName());
        claims.put("email", user.getUserInfo().getEmail());
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);

        return generateToken(claims, user.getUserInfo().getEmail(), jwtExpirationMs);
    }

    public String generateRefreshToken( User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put(USER_ID_CLAIM, user.getId());
        refreshClaims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        return generateToken(refreshClaims, user.getUserInfo().getEmail(), jwtRefreshExpirationMs);
    }

    private String generateToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        extraClaims.put("jti", UUID.randomUUID().toString()); // Ensures the token is unique

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public LocalDateTime extractExpirationDateTime(String token) {
        return LocalDateTime.ofInstant(extractExpiration(token).toInstant(), ZoneId.systemDefault());
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    // Returns null for tokens issued without the "type" claim
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
    }

    // Method to extract all user data at once
    public UserTokenData extractUserData(String token) {
        Claims claims = parseToken(token);
        return UserTokenData.builder()
                .userId(claims.get(USER_ID_CLAIM, Long.class))
                .email(claims.getSubject())
                .role(claims.get("role", String.class))
                .firstName(claims.get("firstName", String.class))
                .lastName(claims.get("lastName", String.class))
                .userName(claims.get("userName", String.class))
                .build();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        if (claimsResolver == null) {
            throw new IllegalArgumentException("Claims resolver cannot be null");
        }
        final Claims claims = parseToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, String userEmail) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userEmail) && !isTokenExpired(token));
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}