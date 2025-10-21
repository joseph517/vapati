package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.*;
import com.vaPaTi.vaPaTi.entity.*;
import com.vaPaTi.vaPaTi.mapper.ReportMapper;
import com.vaPaTi.vaPaTi.repository.ReportRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.ReportValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportValidationService reportValidationService;
    private final ReportMapper reportMapper;
    private final AuthenticatedUserService authenticatedUserService;
    private final ReportActionService reportActionService;

    /**
     * Create a new report
     */
    @Transactional
    public ReportResponseDTO createReport(CreateReportDTO dto) {
        // Get authenticated user
        Long reporterId = authenticatedUserService.getAuthenticatedUserId();

        // Validate input
        reportValidationService.validateInput(dto);

        // Validate not self-report
        reportValidationService.validateNotSelfReport(reporterId, dto.getReportedEntityType(), dto.getReportedEntityId());

        // Validate entity exists and is not deleted
        reportValidationService.validateEntityExists(dto.getReportedEntityType(), dto.getReportedEntityId());

        // Validate no duplicate report
        reportValidationService.validateNoDuplicateReport(reporterId, dto.getReportedEntityType(), dto.getReportedEntityId());

        // Validate daily limit
        reportValidationService.validateDailyReportLimit(reporterId);

        // Get reporter user
        User reporter = reportValidationService.getReporter(reporterId);

        // Create report entity
        Report report = Report.builder()
                .reporter(reporter)
                .reportedEntityType(dto.getReportedEntityType())
                .reportedEntityId(dto.getReportedEntityId())
                .reason(dto.getReason())
                .description(dto.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        // Save report
        reportRepository.save(report);

        return ReportResponseDTO.builder()
                .message("Report submitted successfully")
                .success(true)
                .build();
    }

    /**
     * Get all reports with pagination (ADMIN only)
     */
    public Page<ReportDTO> getAllReports(Pageable pageable) {
        Page<Report> reports = reportRepository.findAll(pageable);
        return reports.map(reportMapper::toDTO);
    }

    /**
     * Get report by ID (ADMIN only)
     */
    public ReportDTO getReportById(Long id) {
        Report report = reportValidationService.validateReportExists(id);
        return reportMapper.toDTO(report);
    }

    /**
     * Review a report (ADMIN only)
     */
    @Transactional
    public ReportDTO reviewReport(Long reportId, ReviewReportDTO dto) {
        // Get authenticated admin user
        Long adminId = authenticatedUserService.getAuthenticatedUserId();

        // Validate report exists
        Report report = reportValidationService.validateReportExists(reportId);

        // Validate review input
        reportValidationService.validateReviewInput(dto.getStatus(), dto.getActionTaken());

        // Get admin user
        User admin = reportValidationService.getReporter(adminId);

        // Update report
        report.setStatus(dto.getStatus());
        report.setAdminNotes(dto.getAdminNotes());
        report.setActionTaken(dto.getActionTaken());
        report.setReviewedBy(admin);
        report.setReviewedAt(LocalDateTime.now());

        // Save report
        Report updatedReport = reportRepository.save(report);

        // Execute the action if specified
        if (dto.getActionTaken() != null && dto.getActionTaken() != ActionTaken.NO_ACTION) {
            reportActionService.executeAction(updatedReport);
        }

        return reportMapper.toDTO(updatedReport);
    }

    /**
     * Get reports created by the authenticated user
     */
    public Page<ReportDTO> getMyReports(Pageable pageable) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        Page<Report> reports = reportRepository.findByReporterId(userId, pageable);
        return reports.map(reportMapper::toDTO);
    }

    /**
     * Get report statistics (ADMIN only)
     */
    public ReportStatsDTO getReportStats() {
        // Total reports
        long totalReports = reportRepository.count();

        // Reports by status
        Map<String, Long> reportsByStatus = new HashMap<>();
        for (ReportStatus status : ReportStatus.values()) {
            long count = reportRepository.countByStatus(status);
            reportsByStatus.put(status.name(), count);
        }

        // Reports by reason
        Map<String, Long> reportsByReason = new HashMap<>();
        for (ReportReason reason : ReportReason.values()) {
            long count = reportRepository.countByReason(reason);
            reportsByReason.put(reason.name(), count);
        }

        // Reports by entity type
        Map<String, Long> reportsByEntityType = new HashMap<>();
        for (ReportedEntityType entityType : ReportedEntityType.values()) {
            long count = reportRepository.countByReportedEntityType(entityType);
            reportsByEntityType.put(entityType.name(), count);
        }

        // Specific counts
        long pendingReports = reportRepository.countByStatus(ReportStatus.PENDING);
        long resolvedReports = reportRepository.countByStatus(ReportStatus.RESOLVED);
        long rejectedReports = reportRepository.countByStatus(ReportStatus.REJECTED);

        return ReportStatsDTO.builder()
                .totalReports(totalReports)
                .reportsByStatus(reportsByStatus)
                .reportsByReason(reportsByReason)
                .reportsByEntityType(reportsByEntityType)
                .pendingReports(pendingReports)
                .resolvedReports(resolvedReports)
                .rejectedReports(rejectedReports)
                .build();
    }
}
