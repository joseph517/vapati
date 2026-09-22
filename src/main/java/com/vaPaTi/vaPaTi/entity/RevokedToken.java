package com.vaPaTi.vaPaTi.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "revoked_tokens")
@Data
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RevokedToken() {}

    public RevokedToken(String jti, LocalDateTime expirationDate) {
        this.jti = jti;
        this.expirationDate = expirationDate;
        this.createdAt = LocalDateTime.now();
    }
}