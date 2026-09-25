package com.vaPaTi.vaPaTi.dtos.report;

import com.vaPaTi.vaPaTi.entity.report.ActionTaken;
import com.vaPaTi.vaPaTi.entity.report.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReportDTO {
    @NotNull
    private ReportStatus status;
    private String adminNotes;
    private ActionTaken actionTaken;
}
