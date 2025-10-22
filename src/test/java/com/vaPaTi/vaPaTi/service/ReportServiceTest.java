package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.*;
import com.vaPaTi.vaPaTi.entity.*;
import com.vaPaTi.vaPaTi.mapper.ReportMapper;
import com.vaPaTi.vaPaTi.repository.ReportRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.ReportValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Tests")
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportValidationService reportValidationService;
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private ReportActionService reportActionService;

    @InjectMocks
    private ReportService reportService;

    private CreateReportDTO createReportDTO;
    private ReviewReportDTO reviewReportDTO;
    private User reporter;
    private User admin;
    private Report report;
    private ReportDTO reportDTO;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        createReportDTO = new CreateReportDTO();
        createReportDTO.setReportedEntityType(ReportedEntityType.USER);
        createReportDTO.setReportedEntityId(2L);
        createReportDTO.setReason(ReportReason.SPAM);
        createReportDTO.setDescription("Test description");

        reviewReportDTO = new ReviewReportDTO();
        reviewReportDTO.setStatus(ReportStatus.RESOLVED);
        reviewReportDTO.setActionTaken(ActionTaken.USER_BANNED);
        reviewReportDTO.setAdminNotes("Admin notes");

        reporter = new User();
        reporter.setId(1L);

        admin = new User();
        admin.setId(10L);

        report = new Report();
        report.setId(1L);
        report.setReporter(reporter);
        report.setReportedEntityType(ReportedEntityType.USER);
        report.setReportedEntityId(2L);
        report.setReason(ReportReason.SPAM);
        report.setDescription("Test description");
        report.setStatus(ReportStatus.PENDING);

        reportDTO = new ReportDTO();
        reportDTO.setId(1L);

        pageable = Pageable.unpaged();
    }

    @Nested
    @DisplayName("createReport() tests")
    class CreateReportTests {

        @Test
        @DisplayName("Should create report successfully with valid data")
        void createReport_WithValidData_ShouldCreateReport() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            when(reportValidationService.getReporter(1L)).thenReturn(reporter);
            when(reportRepository.save(any(Report.class))).thenReturn(report);

            // When
            ReportResponseDTO result = reportService.createReport(createReportDTO);

            // Then
            assertThat(result)
                    .isNotNull()
                    .satisfies(response -> {
                        assertThat(response.isSuccess()).isTrue();
                        assertThat(response.getMessage()).isEqualTo("Report submitted successfully");
                    });

            verify(reportValidationService).validateInput(createReportDTO);
            verify(reportValidationService).validateNotSelfReport(1L, ReportedEntityType.USER, 2L);
            verify(reportValidationService).validateEntityExists(ReportedEntityType.USER, 2L);
            verify(reportValidationService).validateNoDuplicateReport(1L, ReportedEntityType.USER, 2L);
            verify(reportValidationService).validateDailyReportLimit(1L);
            verify(reportRepository).save(any(Report.class));
        }

        @Test
        @DisplayName("Should validate input via validation service")
        void createReport_ShouldValidateInput() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            when(reportValidationService.getReporter(1L)).thenReturn(reporter);

            // When
            reportService.createReport(createReportDTO);

            // Then
            verify(reportValidationService).validateInput(createReportDTO);
        }

        @Test
        @DisplayName("Should validate not self-report via validation service")
        void createReport_ShouldValidateNotSelfReport() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            when(reportValidationService.getReporter(1L)).thenReturn(reporter);

            // When
            reportService.createReport(createReportDTO);

            // Then
            verify(reportValidationService).validateNotSelfReport(1L, ReportedEntityType.USER, 2L);
        }

        @Test
        @DisplayName("Should validate entity exists via validation service")
        void createReport_ShouldValidateEntityExists() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            when(reportValidationService.getReporter(1L)).thenReturn(reporter);

            // When
            reportService.createReport(createReportDTO);

            // Then
            verify(reportValidationService).validateEntityExists(ReportedEntityType.USER, 2L);
        }

        @Test
        @DisplayName("Should validate no duplicate report via validation service")
        void createReport_ShouldValidateNoDuplicate() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            when(reportValidationService.getReporter(1L)).thenReturn(reporter);

            // When
            reportService.createReport(createReportDTO);

            // Then
            verify(reportValidationService).validateNoDuplicateReport(1L, ReportedEntityType.USER, 2L);
        }

        @Test
        @DisplayName("Should validate daily report limit via validation service")
        void createReport_ShouldValidateDailyLimit() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            when(reportValidationService.getReporter(1L)).thenReturn(reporter);

            // When
            reportService.createReport(createReportDTO);

            // Then
            verify(reportValidationService).validateDailyReportLimit(1L);
        }

        @Test
        @DisplayName("Should set report status to PENDING")
        void createReport_ShouldSetStatusToPending() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            when(reportValidationService.getReporter(1L)).thenReturn(reporter);
            when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
                Report savedReport = invocation.getArgument(0);
                assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.PENDING);
                return savedReport;
            });

            // When
            reportService.createReport(createReportDTO);

            // Then
            verify(reportRepository).save(any(Report.class));
        }

        @Test
        @DisplayName("Should execute validations in correct order")
        void createReport_ShouldExecuteValidationsInOrder() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            when(reportValidationService.getReporter(1L)).thenReturn(reporter);
            InOrder inOrder = inOrder(reportValidationService);

            // When
            reportService.createReport(createReportDTO);

            // Then
            inOrder.verify(reportValidationService).validateInput(createReportDTO);
            inOrder.verify(reportValidationService).validateNotSelfReport(1L, ReportedEntityType.USER, 2L);
            inOrder.verify(reportValidationService).validateEntityExists(ReportedEntityType.USER, 2L);
            inOrder.verify(reportValidationService).validateNoDuplicateReport(1L, ReportedEntityType.USER, 2L);
            inOrder.verify(reportValidationService).validateDailyReportLimit(1L);
        }
    }

    @Nested
    @DisplayName("getAllReports() tests")
    class GetAllReportsTests {

        @Test
        @DisplayName("Should return paginated reports")
        void getAllReports_ShouldReturnPaginatedReports() {
            // Given
            Page<Report> reportPage = new PageImpl<>(List.of(report));
            when(reportRepository.findAll(pageable)).thenReturn(reportPage);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            Page<ReportDTO> result = reportService.getAllReports(pageable);

            // Then
            assertThat(result)
                    .isNotNull();
            assertThat(result.getContent())
                    .hasSize(1)
                    .element(0).isEqualTo(reportDTO);

            verify(reportRepository).findAll(pageable);
            verify(reportMapper).toDTO(report);
        }

        @Test
        @DisplayName("Should map reports to DTOs")
        void getAllReports_ShouldMapReportsToDTOs() {
            // Given
            Page<Report> reportPage = new PageImpl<>(List.of(report));
            when(reportRepository.findAll(pageable)).thenReturn(reportPage);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.getAllReports(pageable);

            // Then
            verify(reportMapper).toDTO(report);
        }

        @Test
        @DisplayName("Should handle pagination parameters")
        void getAllReports_ShouldHandlePagination() {
            // Given
            Page<Report> reportPage = new PageImpl<>(List.of());
            when(reportRepository.findAll(pageable)).thenReturn(reportPage);

            // When
            reportService.getAllReports(pageable);

            // Then
            verify(reportRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("getReportById() tests")
    class GetReportByIdTests {

        @Test
        @DisplayName("Should return report DTO when report exists")
        void getReportById_WithValidId_ShouldReturnReportDTO() {
            // Given
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            ReportDTO result = reportService.getReportById(1L);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(reportDTO);

            verify(reportValidationService).validateReportExists(1L);
            verify(reportMapper).toDTO(report);
        }

        @Test
        @DisplayName("Should delegate to validation service for existence check")
        void getReportById_ShouldDelegateToValidationService() {
            // Given
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.getReportById(1L);

            // Then
            verify(reportValidationService).validateReportExists(1L);
        }

        @Test
        @DisplayName("Should use mapper to create DTO")
        void getReportById_ShouldUseMapper() {
            // Given
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.getReportById(1L);

            // Then
            verify(reportMapper).toDTO(report);
        }
    }

    @Nested
    @DisplayName("reviewReport() tests")
    class ReviewReportTests {

        @Test
        @DisplayName("Should review report successfully")
        void reviewReport_WithValidData_ShouldReviewReport() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            ReportDTO result = reportService.reviewReport(1L, reviewReportDTO);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(reportDTO);

            verify(reportValidationService).validateReportExists(1L);
            verify(reportValidationService).validateReviewInput(ReportStatus.RESOLVED, ActionTaken.USER_BANNED);
            verify(reportRepository).save(report);
            verify(reportActionService).executeAction(report);
        }

        @Test
        @DisplayName("Should validate report exists")
        void reviewReport_ShouldValidateReportExists() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.reviewReport(1L, reviewReportDTO);

            // Then
            verify(reportValidationService).validateReportExists(1L);
        }

        @Test
        @DisplayName("Should validate review input")
        void reviewReport_ShouldValidateReviewInput() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.reviewReport(1L, reviewReportDTO);

            // Then
            verify(reportValidationService).validateReviewInput(ReportStatus.RESOLVED, ActionTaken.USER_BANNED);
        }

        @Test
        @DisplayName("Should update report status")
        void reviewReport_ShouldUpdateStatus() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.reviewReport(1L, reviewReportDTO);

            // Then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        }

        @Test
        @DisplayName("Should update admin notes")
        void reviewReport_ShouldUpdateAdminNotes() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.reviewReport(1L, reviewReportDTO);

            // Then
            assertThat(report.getAdminNotes()).isEqualTo("Admin notes");
        }

        @Test
        @DisplayName("Should update action taken")
        void reviewReport_ShouldUpdateActionTaken() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.reviewReport(1L, reviewReportDTO);

            // Then
            assertThat(report.getActionTaken()).isEqualTo(ActionTaken.USER_BANNED);
        }

        @Test
        @DisplayName("Should set reviewedBy to admin user")
        void reviewReport_ShouldSetReviewedBy() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.reviewReport(1L, reviewReportDTO);

            // Then
            assertThat(report.getReviewedBy()).isEqualTo(admin);
        }

        @Test
        @DisplayName("Should set reviewedAt timestamp")
        void reviewReport_ShouldSetReviewedAt() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);
            LocalDateTime before = LocalDateTime.now();

            // When
            reportService.reviewReport(1L, reviewReportDTO);

            // Then
            assertThat(report.getReviewedAt()).isNotNull();
            assertThat(report.getReviewedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("Should execute action when actionTaken is not NO_ACTION")
        void reviewReport_WithAction_ShouldExecuteAction() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.reviewReport(1L, reviewReportDTO);

            // Then
            verify(reportActionService).executeAction(report);
        }

        @Test
        @DisplayName("Should not execute action when actionTaken is NO_ACTION")
        void reviewReport_WithNoAction_ShouldNotExecuteAction() {
            // Given
            reviewReportDTO.setActionTaken(ActionTaken.NO_ACTION);
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(10L);
            when(reportValidationService.validateReportExists(1L)).thenReturn(report);
            when(reportValidationService.getReporter(10L)).thenReturn(admin);
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.reviewReport(1L, reviewReportDTO);

            // Then
            verify(reportActionService, never()).executeAction(any());
        }
    }

    @Nested
    @DisplayName("getMyReports() tests")
    class GetMyReportsTests {

        @Test
        @DisplayName("Should return user's reports with pagination")
        void getMyReports_ShouldReturnUserReports() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            Page<Report> reportPage = new PageImpl<>(List.of(report));
            when(reportRepository.findByReporterId(1L, pageable)).thenReturn(reportPage);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            Page<ReportDTO> result = reportService.getMyReports(pageable);

            // Then
            assertThat(result)
                    .isNotNull();
            assertThat(result.getContent())
                    .hasSize(1)
                    .element(0).isEqualTo(reportDTO);

            verify(reportRepository).findByReporterId(1L, pageable);
        }

        @Test
        @DisplayName("Should filter by authenticated user ID")
        void getMyReports_ShouldFilterByUserId() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            Page<Report> reportPage = new PageImpl<>(List.of());
            when(reportRepository.findByReporterId(1L, pageable)).thenReturn(reportPage);

            // When
            reportService.getMyReports(pageable);

            // Then
            verify(reportRepository).findByReporterId(1L, pageable);
        }

        @Test
        @DisplayName("Should map reports to DTOs")
        void getMyReports_ShouldMapReportsToDTOs() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(1L);
            Page<Report> reportPage = new PageImpl<>(List.of(report));
            when(reportRepository.findByReporterId(1L, pageable)).thenReturn(reportPage);
            when(reportMapper.toDTO(report)).thenReturn(reportDTO);

            // When
            reportService.getMyReports(pageable);

            // Then
            verify(reportMapper).toDTO(report);
        }
    }

    @Nested
    @DisplayName("getReportStats() tests")
    class GetReportStatsTests {

        @Test
        @DisplayName("Should return complete statistics")
        void getReportStats_ShouldReturnCompleteStats() {
            // Given
            when(reportRepository.count()).thenReturn(100L);
            for (ReportStatus status : ReportStatus.values()) {
                when(reportRepository.countByStatus(status)).thenReturn(10L);
            }
            for (ReportReason reason : ReportReason.values()) {
                when(reportRepository.countByReason(reason)).thenReturn(5L);
            }
            for (ReportedEntityType entityType : ReportedEntityType.values()) {
                when(reportRepository.countByReportedEntityType(entityType)).thenReturn(15L);
            }

            // When
            ReportStatsDTO result = reportService.getReportStats();

            // Then
            assertThat(result)
                    .isNotNull()
                    .satisfies(stats -> {
                        assertThat(stats.getTotalReports()).isEqualTo(100L);
                        assertThat(stats.getReportsByStatus()).isNotEmpty();
                        assertThat(stats.getReportsByReason()).isNotEmpty();
                        assertThat(stats.getReportsByEntityType()).isNotEmpty();
                    });
        }

        @Test
        @DisplayName("Should calculate total reports")
        void getReportStats_ShouldCalculateTotalReports() {
            // Given
            when(reportRepository.count()).thenReturn(100L);
            mockStatsRepositoryCalls();

            // When
            ReportStatsDTO result = reportService.getReportStats();

            // Then
            assertThat(result.getTotalReports()).isEqualTo(100L);
            verify(reportRepository).count();
        }

        @Test
        @DisplayName("Should calculate reports by status")
        void getReportStats_ShouldCalculateReportsByStatus() {
            // Given
            when(reportRepository.count()).thenReturn(100L);
            mockStatsRepositoryCalls();

            // When
            ReportStatsDTO result = reportService.getReportStats();

            // Then
            assertThat(result.getReportsByStatus()).hasSize(ReportStatus.values().length);
            for (ReportStatus status : ReportStatus.values()) {
                verify(reportRepository, atLeastOnce()).countByStatus(status);
            }
        }

        @Test
        @DisplayName("Should calculate reports by reason")
        void getReportStats_ShouldCalculateReportsByReason() {
            // Given
            when(reportRepository.count()).thenReturn(100L);
            mockStatsRepositoryCalls();

            // When
            ReportStatsDTO result = reportService.getReportStats();

            // Then
            assertThat(result.getReportsByReason()).hasSize(ReportReason.values().length);
            for (ReportReason reason : ReportReason.values()) {
                verify(reportRepository).countByReason(reason);
            }
        }

        @Test
        @DisplayName("Should calculate reports by entity type")
        void getReportStats_ShouldCalculateReportsByEntityType() {
            // Given
            when(reportRepository.count()).thenReturn(100L);
            mockStatsRepositoryCalls();

            // When
            ReportStatsDTO result = reportService.getReportStats();

            // Then
            assertThat(result.getReportsByEntityType()).hasSize(ReportedEntityType.values().length);
            for (ReportedEntityType entityType : ReportedEntityType.values()) {
                verify(reportRepository).countByReportedEntityType(entityType);
            }
        }

        @Test
        @DisplayName("Should calculate pending reports count")
        void getReportStats_ShouldCalculatePendingReports() {
            // Given
            when(reportRepository.count()).thenReturn(100L);
            when(reportRepository.countByStatus(ReportStatus.PENDING)).thenReturn(10L);
            mockStatsRepositoryCalls();

            // When
            ReportStatsDTO result = reportService.getReportStats();

            // Then
            assertThat(result.getPendingReports()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should iterate through all enum values")
        void getReportStats_ShouldIterateThroughAllEnums() {
            // Given
            when(reportRepository.count()).thenReturn(100L);
            mockStatsRepositoryCalls();

            // When
            ReportStatsDTO result = reportService.getReportStats();

            // Then
            assertThat(result.getReportsByStatus().keySet())
                    .hasSize(ReportStatus.values().length);
            assertThat(result.getReportsByReason().keySet())
                    .hasSize(ReportReason.values().length);
            assertThat(result.getReportsByEntityType().keySet())
                    .hasSize(ReportedEntityType.values().length);
        }

        private void mockStatsRepositoryCalls() {
            for (ReportStatus status : ReportStatus.values()) {
                when(reportRepository.countByStatus(status)).thenReturn(10L);
            }
            for (ReportReason reason : ReportReason.values()) {
                when(reportRepository.countByReason(reason)).thenReturn(5L);
            }
            for (ReportedEntityType entityType : ReportedEntityType.values()) {
                when(reportRepository.countByReportedEntityType(entityType)).thenReturn(15L);
            }
        }
    }
}
