package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.ActionTaken;
import com.vaPaTi.vaPaTi.entity.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReportDTO {
    private ReportStatus status;
    private String adminNotes;
    private ActionTaken actionTaken;
}
