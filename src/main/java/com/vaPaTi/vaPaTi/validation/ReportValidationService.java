package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.CreateReportDTO;
import com.vaPaTi.vaPaTi.entity.*;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.ReportRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportValidationService {

    private static final int MAX_REPORTS_PER_DAY = 10;
    private static final String USER_NOT_FOUND = "User not found";
    private static final String PUBLICATION_NOT_FOUND = "Publication not found";
    private static final String CAMPAIGN_NOT_FOUND = "Campaign not found";

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final CampaignRepository campaignRepository;

    /**
     * Validate the input DTO
     */
    public void validateInput(CreateReportDTO dto) {
        if (dto == null) {
            throw new MessageException("Report data cannot be null");
        }

        if (dto.getReportedEntityType() == null) {
            throw new MessageException("Reported entity type is required");
        }

        if (dto.getReportedEntityId() == null || dto.getReportedEntityId() <= 0) {
            throw new MessageException("Valid reported entity ID is required");
        }

        if (dto.getReason() == null) {
            throw new MessageException("Report reason is required");
        }

        if (dto.getDescription() != null && dto.getDescription().length() > 1000) {
            throw new MessageException("Description cannot exceed 1000 characters");
        }
    }

    /**
     * Validate that user is not reporting themselves
     */
    public void validateNotSelfReport(Long reporterId, ReportedEntityType entityType, Long entityId) {
        if (entityType == ReportedEntityType.USER && reporterId.equals(entityId)) {
            throw new MessageException("You cannot report yourself");
        }
    }

    /**
     * Validate that the reported entity exists and is not deleted
     */
    public void validateEntityExists(ReportedEntityType entityType, Long entityId) {
        switch (entityType) {
            case USER -> {
                Optional<User> user = userRepository.findById(entityId);
                if (user.isEmpty()) {
                    throw new MessageException(USER_NOT_FOUND);
                }
                if (user.get().getDeletedAt() != null) {
                    throw new MessageException("Cannot report a deleted user");
                }
            }
            case PUBLICATION -> {
                Optional<Publication> publication = publicationRepository.findById(entityId);
                if (publication.isEmpty()) {
                    throw new MessageException(PUBLICATION_NOT_FOUND);
                }
                if (publication.get().getDeletedAt() != null) {
                    throw new MessageException("Cannot report a deleted publication");
                }
            }
            case CAMPAIGN -> {
                Optional<Campaign> campaign = campaignRepository.findById(entityId);
                if (campaign.isEmpty()) {
                    throw new MessageException(CAMPAIGN_NOT_FOUND);
                }
                // Campaign doesn't have soft delete yet, but we check if it exists
            }
            default -> throw new MessageException("Invalid entity type");
        }
    }

    /**
     * Validate that there's no duplicate report
     */
    public void validateNoDuplicateReport(Long reporterId, ReportedEntityType entityType, Long entityId) {
        boolean exists = reportRepository.existsByReporterIdAndReportedEntityTypeAndReportedEntityId(
                reporterId, entityType, entityId
        );

        if (exists) {
            throw new MessageException("You have already reported this " + entityType.name().toLowerCase());
        }
    }

    /**
     * Validate daily report limit to prevent abuse
     */
    public void validateDailyReportLimit(Long reporterId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Long reportsCount = reportRepository.countReportsByReporterIdSince(reporterId, since);

        if (reportsCount >= MAX_REPORTS_PER_DAY) {
            throw new MessageException("You have reached the maximum number of reports allowed per day (" + MAX_REPORTS_PER_DAY + ")");
        }
    }

    /**
     * Validate that report exists
     */
    public Report validateReportExists(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new MessageException("Report not found with id: " + reportId));
    }

    /**
     * Validate review input
     */
    public void validateReviewInput(ReportStatus status, ActionTaken actionTaken) {
        if (status == null) {
            throw new MessageException("Status is required for review");
        }

        // If status is RESOLVED, action taken should be specified
        if (status == ReportStatus.RESOLVED && (actionTaken == null || actionTaken == ActionTaken.NO_ACTION)) {
            throw new MessageException("Action taken must be specified when resolving a report");
        }

        // PENDING reports cannot be reviewed (they need to go to UNDER_REVIEW first or directly to RESOLVED/REJECTED)
        if (status == ReportStatus.PENDING) {
            throw new MessageException("Cannot set status back to PENDING");
        }
    }

    /**
     * Get the reporter user entity
     */
    public User getReporter(Long reporterId) {
        return userRepository.findById(reporterId)
                .orElseThrow(() -> new MessageException("Reporter " + USER_NOT_FOUND));
    }
}
