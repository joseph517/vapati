package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.VerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {

    @Query("SELECT vr FROM VerificationRequest vr WHERE vr.user.id = :userId")
    Optional<VerificationRequest> findByUserId(@Param("userId") Long userId);

}