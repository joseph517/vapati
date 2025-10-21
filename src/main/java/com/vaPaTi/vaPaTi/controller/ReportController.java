package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.*;
import com.vaPaTi.vaPaTi.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report management API")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "Create a report", description = "Create a new report for a user, publication, or campaign")
    public ResponseEntity<ReportResponseDTO> createReport(@RequestBody CreateReportDTO dto) {
        ReportResponseDTO response = reportService.createReport(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all reports", description = "Get all reports with pagination (ADMIN only)")
    public ResponseEntity<Page<ReportDTO>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReportDTO> reports = reportService.getAllReports(pageable);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get report by ID", description = "Get a specific report by ID (ADMIN only)")
    public ResponseEntity<ReportDTO> getReportById(@PathVariable Long id) {
        ReportDTO report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Review a report", description = "Review and update the status of a report (ADMIN only)")
    public ResponseEntity<ReportDTO> reviewReport(
            @PathVariable Long id,
            @RequestBody ReviewReportDTO dto
    ) {
        ReportDTO report = reportService.reviewReport(id, dto);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/my-reports")
    @Operation(summary = "Get my reports", description = "Get all reports created by the authenticated user")
    public ResponseEntity<Page<ReportDTO>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReportDTO> reports = reportService.getMyReports(pageable);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get report statistics", description = "Get aggregated statistics about reports (ADMIN only)")
    public ResponseEntity<ReportStatsDTO> getReportStats() {
        ReportStatsDTO stats = reportService.getReportStats();
        return ResponseEntity.ok(stats);
    }
}
