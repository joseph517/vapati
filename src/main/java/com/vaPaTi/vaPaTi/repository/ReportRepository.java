package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.Report;
import com.vaPaTi.vaPaTi.entity.ReportReason;
import com.vaPaTi.vaPaTi.entity.ReportStatus;
import com.vaPaTi.vaPaTi.entity.ReportedEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Check if a report already exists for a specific reporter, entity type and entity id
     */
    boolean existsByReporterIdAndReportedEntityTypeAndReportedEntityId(
            Long reporterId,
            ReportedEntityType reportedEntityType,
            Long reportedEntityId
    );

    /**
     * Find a report by reporter, entity type and entity id
     */
    Optional<Report> findByReporterIdAndReportedEntityTypeAndReportedEntityId(
            Long reporterId,
            ReportedEntityType reportedEntityType,
            Long reportedEntityId
    );

    /**
     * Get all reports with pagination
     * Order is controlled by Pageable parameter from controller
     */
    Page<Report> findAll(Pageable pageable);

    /**
     * Get all reports with pagination, loading reporter and reviewedBy (each with its inverse @OneToOne) in the same
     * SELECT. The joins are LEFT: a soft-deleted reporter or reviewer stays null (@NotFound) instead of removing the
     * row. Only ToOne relations are fetched, so pagination stays in SQL. The order comes from the Pageable
     * (Report columns only); the count can't be derived from a FETCH query, so it is explicit
     */
    @Query(value = "SELECT r FROM Report r " +
            "LEFT JOIN FETCH r.reporter rp LEFT JOIN FETCH rp.userInfo LEFT JOIN FETCH rp.verificationRequest " +
            "LEFT JOIN FETCH r.reviewedBy rv LEFT JOIN FETCH rv.userInfo LEFT JOIN FETCH rv.verificationRequest",
            countQuery = "SELECT COUNT(r) FROM Report r")
    Page<Report> findAllWithUsers(Pageable pageable);

    /**
     * Get reports by status with pagination
     */
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    /**
     * Count reports created by a user in the last 24 hours
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.reporter.id = :reporterId AND r.createdAt >= :since")
    Long countReportsByReporterIdSince(@Param("reporterId") Long reporterId, @Param("since") LocalDateTime since);

    /**
     * Count total reports
     */
    long count();

    /**
     * Count reports by status
     */
    long countByStatus(ReportStatus status);

    /**
     * Count reports by reason
     */
    long countByReason(ReportReason reason);

    /**
     * Count reports by entity type
     */
    long countByReportedEntityType(ReportedEntityType entityType);

    /**
     * Get all reports for a specific entity
     */
    @Query("SELECT r FROM Report r WHERE r.reportedEntityType = :entityType AND r.reportedEntityId = :entityId ORDER BY r.createdAt DESC")
    Page<Report> findReportsByEntity(
            @Param("entityType") ReportedEntityType entityType,
            @Param("entityId") Long entityId,
            Pageable pageable
    );
}
