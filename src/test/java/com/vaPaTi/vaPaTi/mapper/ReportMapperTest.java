package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.ReportDTO;
import com.vaPaTi.vaPaTi.entity.Report;
import com.vaPaTi.vaPaTi.entity.ReportReason;
import com.vaPaTi.vaPaTi.entity.ReportStatus;
import com.vaPaTi.vaPaTi.entity.ReportedEntityType;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("ReportMapper Tests")
class ReportMapperTest {

    private final ReportMapper reportMapper = new ReportMapper();

    private Report report;

    @BeforeEach
    void setUp() {
        report = Report.builder()
                .id(1L)
                .reportedEntityType(ReportedEntityType.USER)
                .reportedEntityId(2L)
                .reason(ReportReason.SPAM)
                .status(ReportStatus.RESOLVED)
                .build();
    }

    private User userWithName(Long id, String userName) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName(userName);
        userInfo.setEmail(userName + "@example.com");
        User user = new User();
        user.setId(id);
        user.setUserInfo(userInfo);
        return user;
    }

    @Test
    @DisplayName("Should map reporter and reviewer when both are alive")
    void toDTO_WithAliveUsers_ShouldMapUserFields() {
        // Given
        report.setReporter(userWithName(1L, "reporter"));
        report.setReviewedBy(userWithName(10L, "admin"));

        // When
        ReportDTO dto = reportMapper.toDTO(report);

        // Then
        assertThat(dto.getReporterId()).isEqualTo(1L);
        assertThat(dto.getReporterUsername()).isEqualTo("reporter");
        assertThat(dto.getReporterEmail()).isEqualTo("reporter@example.com");
        assertThat(dto.getReviewedById()).isEqualTo(10L);
        assertThat(dto.getReviewedByUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Should not throw and leave reporter fields null when the reporter is deleted")
    void toDTO_WithDeletedReporter_ShouldReturnNullReporterFields() {
        // Given
        report.setReporter(null);
        report.setReviewedBy(userWithName(10L, "admin"));

        // When & Then
        assertThatCode(() -> reportMapper.toDTO(report)).doesNotThrowAnyException();
        ReportDTO dto = reportMapper.toDTO(report);
        assertThat(dto.getReporterId()).isNull();
        assertThat(dto.getReporterUsername()).isNull();
        assertThat(dto.getReporterEmail()).isNull();
        assertThat(dto.getReviewedById()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should not throw and leave reviewer fields null when the reviewer is deleted")
    void toDTO_WithDeletedReviewer_ShouldReturnNullReviewerFields() {
        // Given
        report.setReporter(userWithName(1L, "reporter"));
        report.setReviewedBy(null);

        // When & Then
        assertThatCode(() -> reportMapper.toDTO(report)).doesNotThrowAnyException();
        ReportDTO dto = reportMapper.toDTO(report);
        assertThat(dto.getReviewedById()).isNull();
        assertThat(dto.getReviewedByUsername()).isNull();
        assertThat(dto.getReporterId()).isEqualTo(1L);
        assertThat(dto.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }
}
