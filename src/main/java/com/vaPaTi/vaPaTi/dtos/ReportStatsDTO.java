package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportStatsDTO {
    private Long totalReports;
    private Map<String, Long> reportsByStatus;
    private Map<String, Long> reportsByReason;
    private Map<String, Long> reportsByEntityType;
    private Long pendingReports;
    private Long resolvedReports;
    private Long rejectedReports;
}
