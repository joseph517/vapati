package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.UserTokenData;
import com.vaPaTi.vaPaTi.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    private Key signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken( User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        Map<String, Object> claims = new HashMap<>();
        // Agregar datos del usuario al token
        claims.put("role", user.getRole().getName());
        claims.put("userId", user.getId());
        claims.put("firstName", user.getUserInfo().getFirstName());
        claims.put("lastName", user.getUserInfo().getLastName());
        claims.put("userName", user.getUserInfo().getUserName());
        claims.put("email", user.getUserInfo().getEmail());

        return generateToken(claims, user.getUserInfo().getEmail(), jwtExpirationMs);
    }

    public String generateRefreshToken( User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put("userId", user.getId());
        return generateToken(refreshClaims, user.getUserInfo().getEmail(), jwtRefreshExpirationMs);
    }

    private String generateToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        extraClaims.put("jti", UUID.randomUUID().toString()); // Asegura que el token sea único

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

    // Método para extraer todos los datos del usuario de una vez
    public UserTokenData extractUserData(String token) {
        Claims claims = parseToken(token);
        return UserTokenData.builder()
                .userId(claims.get("userId", Long.class))
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