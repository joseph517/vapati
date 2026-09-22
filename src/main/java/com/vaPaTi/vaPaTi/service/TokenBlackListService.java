package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.entity.RevokedToken;
import com.vaPaTi.vaPaTi.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenBlackListService {

    private final RevokedTokenRepository revokedTokenRepository;

    public boolean isTokenRevoked(String jti) {
        return revokedTokenRepository.existsByJti(jti);
    }

    @Transactional
    public void revokeToken(String jti, LocalDateTime expirationDate) {
        if (!isTokenRevoked(jti)) {
            revokedTokenRepository.save(new RevokedToken(jti, expirationDate));
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        revokedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
