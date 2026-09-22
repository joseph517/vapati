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
     * Ban the reported user permanently
     */
    private void executeBan(Report report) {
        // Only ban if the reported entity is a user
        if (report.getReportedEntityType() != ReportedEntityType.USER) {
            return;
        }

        User user = userRepository.findById(report.getReportedEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setBanned(true);
        user.setBannedAt(LocalDateTime.now());
        user.setBannedReason(report.getAdminNotes() != null ? report.getAdminNotes() : "Banned by admin");

        userRepository.save(user);
    }

    /**
     * Suspend the reported user temporarily (30 days)
     */
    private void executeSuspension(Report report) {
        // Only suspend if the reported entity is a user
        if (report.getReportedEntityType() != ReportedEntityType.USER) {
            return;
        }

        User user = userRepository.findById(report.getReportedEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setSuspendedUntil(LocalDateTime.now().plusDays(30));
        user.setBannedReason(report.getAdminNotes() != null ? report.getAdminNotes() : "Suspended by admin");

        userRepository.save(user);
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
     * Soft delete a publication
     */
    private void removePublication(Long publicationId) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Publication not found"));

        // Soft delete is handled by @SQLDelete annotation
        publicationRepository.delete(publication);
    }

    /**
     * Close and soft delete a campaign, recording the transition with the reviewing admin.
     * Uses findById (not the active-owner lookup) so an admin can still resolve a report
     * after the campaign owner deleted their account.
     */
    private void removeCampaign(Long campaignId, Long adminId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        campaignService.closeAndSoftDelete(campaign, adminId);
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
