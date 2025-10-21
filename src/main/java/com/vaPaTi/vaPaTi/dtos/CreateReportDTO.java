package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.ReportReason;
import com.vaPaTi.vaPaTi.entity.ReportedEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportDTO {
    private ReportedEntityType reportedEntityType;
    private Long reportedEntityId;
    private ReportReason reason;
    private String description;
}
