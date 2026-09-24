package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.entity.*;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.PublicationRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for executing moderation actions when a report is reviewed by an admin.
 * This service handles the actual enforcement of actions like banning users, removing content, etc.
 */
@Service
@RequiredArgsConstructor
public class ReportActionService {

    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignService campaignService;

    /**
     * Execute the action specified in the report
     */
    @Transactional
    public void executeAction(Report report) {
        if (report.getActionTaken() == null) {
            return;
        }

        switch (report.getActionTaken()) {
            case USER_BANNED:
                executeBan(report);
                break;
            case USER_SUSPENDED:
                executeSuspension(report);
                break;
            case CONTENT_REMOVED:
                executeContentRemoval(report);
                break;
            case WARNING_SENT:
                executeWarning(report);
                break;
            case NO_ACTION:
            case OTHER:
                // No action needed
                break;
        }
    }

    /**
     * Ban the reported user permanently.
     * Also reaches a deleted account, so logging in cannot restore it. Banning twice keeps the original ban.
     */
    private void executeBan(Report report) {
        // Only ban if the reported entity is a user
        if (report.getReportedEntityType() != ReportedEntityType.USER) {
            return;
        }

        User user = findReportedUser(report.getReportedEntityId());

        if (Boolean.TRUE.equals(user.getBanned())) {
            return;
        }

        user.setBanned(true);
        user.setBannedAt(LocalDateTime.now());
        user.setBannedReason(report.getAdminNotes() != null ? report.getAdminNotes() : "Banned by admin");

        userRepository.save(user);
    }

    /**
     * Suspend the reported user temporarily (30 days).
     * Also reaches a deleted account. A banned user or a running suspension is left as is.
     */
    private void executeSuspension(Report report) {
        // Only suspend if the reported entity is a user
        if (report.getReportedEntityType() != ReportedEntityType.USER) {
            return;
        }

        User user = findReportedUser(report.getReportedEntityId());

        LocalDateTime now = LocalDateTime.now();
        boolean suspended = user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(now);
        if (Boolean.TRUE.equals(user.getBanned()) || suspended) {
            return;
        }

        user.setSuspendedUntil(now.plusDays(30));
        user.setBannedReason(report.getAdminNotes() != null ? report.getAdminNotes() : "Suspended by admin");

        userRepository.save(user);
    }

    // Sanctions only touch the sanction columns, so deletedAt stays as it is
    private User findReportedUser(Long userId) {
        return userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Remove the reported content (Publication or Campaign)
     */
    private void executeContentRemoval(Report report) {
        switch (report.getReportedEntityType()) {
            case PUBLICATION:
                removePublication(report.getReportedEntityId());
                break;
            case CAMPAIGN:
                removeCampaign(report.getReportedEntityId(), report.getReviewedBy().getId());
                break;
            case USER:
                // Cannot remove a user directly, only ban/suspend
                break;
        }
    }

    /**
     * Soft delete a publication.
     * A report always points to an entity that existed, so an empty lookup means it was already removed: nothing to do.
     */
    private void removePublication(Long publicationId) {
        // Soft delete is handled by @SQLDelete annotation
        publicationRepository.findById(publicationId).ifPresent(publicationRepository::delete);
    }

    /**
     * Close and soft delete a campaign, recording the transition with the reviewing admin.
     * Uses findById (not the active-owner lookup) so an admin can still resolve a report
     * after the campaign owner deleted their account. An empty lookup means it was already removed.
     */
    private void removeCampaign(Long campaignId, Long adminId) {
        campaignRepository.findById(campaignId)
                .ifPresent(campaign -> campaignService.closeAndSoftDelete(campaign, adminId));
    }

    /**
     * Record a warning (prepared for future email implementation)
     */
    private void executeWarning(Report report) {
        // For now, just log that a warning should be sent
        // In the future, this would trigger an email notification
        // The warning is already recorded in the report's adminNotes
    }
}
