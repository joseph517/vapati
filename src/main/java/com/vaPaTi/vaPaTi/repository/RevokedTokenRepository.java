package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.RevokedToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByToken(String token);

    @Query("DELETE FROM RevokedToken r WHERE r.expirationDate < :currentDate")
    @Modifying
    @Transactional
    void deleteExpiredTokens(@Param("currentDate") LocalDateTime currentDate);

    Optional<RevokedToken> findByToken(String token);
}
