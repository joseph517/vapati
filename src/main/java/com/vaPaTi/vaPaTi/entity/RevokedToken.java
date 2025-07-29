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

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RevokedToken() {}

    public RevokedToken(String token, LocalDateTime expirationDate) {
        this.token = token;
        this.expirationDate = expirationDate;
        this.createdAt = LocalDateTime.now();
    }
}