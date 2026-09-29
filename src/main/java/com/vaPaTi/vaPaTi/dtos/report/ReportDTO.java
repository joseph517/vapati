package com.vaPaTi.vaPaTi.dtos.report;

import com.vaPaTi.vaPaTi.entity.report.ActionTaken;
import com.vaPaTi.vaPaTi.entity.report.ReportReason;
import com.vaPaTi.vaPaTi.entity.report.ReportStatus;
import com.vaPaTi.vaPaTi.entity.report.ReportedEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDTO {
    private Long id;
    private Long reporterId;
    private String reporterUsername;
    private String reporterEmail;
    private ReportedEntityType reportedEntityType;
    private Long reportedEntityId;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private Long reviewedById;
    private String reviewedByUsername;
    private LocalDateTime reviewedAt;
    private String adminNotes;
    private ActionTaken actionTaken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
