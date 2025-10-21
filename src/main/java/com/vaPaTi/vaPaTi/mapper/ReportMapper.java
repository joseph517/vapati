package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.ReportDTO;
import com.vaPaTi.vaPaTi.entity.Report;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public ReportDTO toDTO(Report report) {
        if (report == null) {
            return null;
        }

        return ReportDTO.builder()
                .id(report.getId())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterUsername(report.getReporter() != null && report.getReporter().getUserInfo() != null
                        ? report.getReporter().getUserInfo().getUserName()
                        : null)
                .reporterEmail(report.getReporter() != null && report.getReporter().getUserInfo() != null
                        ? report.getReporter().getUserInfo().getEmail()
                        : null)
                .reportedEntityType(report.getReportedEntityType())
                .reportedEntityId(report.getReportedEntityId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .reviewedById(report.getReviewedBy() != null ? report.getReviewedBy().getId() : null)
                .reviewedByUsername(report.getReviewedBy() != null && report.getReviewedBy().getUserInfo() != null
                        ? report.getReviewedBy().getUserInfo().getUserName()
                        : null)
                .reviewedAt(report.getReviewedAt())
                .adminNotes(report.getAdminNotes())
                .actionTaken(report.getActionTaken())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
