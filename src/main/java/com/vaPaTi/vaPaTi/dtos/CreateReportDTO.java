package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.ReportReason;
import com.vaPaTi.vaPaTi.entity.ReportedEntityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportDTO {
    @NotNull
    private ReportedEntityType reportedEntityType;
    @NotNull
    @Positive
    private Long reportedEntityId;
    @NotNull
    private ReportReason reason;
    @Size(max = 1000)
    private String description;
}
