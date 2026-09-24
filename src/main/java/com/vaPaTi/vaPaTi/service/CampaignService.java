package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CampaignStatusHistoryResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignCategory;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.CampaignMapper;
import com.vaPaTi.vaPaTi.mapper.CampaignStatusHistoryMapper;
import com.vaPaTi.vaPaTi.repository.CampaignCategoryRepository;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.CampaignStatusHistoryRepository;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.CampaignServiceValidation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final CampaignServiceValidation campaignServiceValidation;
    private final CampaignAuthorizationService campaignAuthorizationService;
    private final CampaignCategoryRepository campaignCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final CampaignStatusHistoryService campaignStatusHistoryService;
    private final CampaignStatusHistoryRepository campaignStatusHistoryRepository;
    private final EntityManager entityManager;

    // Public listings: admins see every campaign, everyone else (anonymous included) only their own CLOSED ones
    public List<CampaignResponseDTO> getAllCampaigns() {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        List<Campaign> campaigns = campaignAuthorizationService.isAdmin(callerId)
                ? campaignRepository.findAllWithActiveOwner()
                : campaignRepository.findAllVisibleTo(callerId);

        return campaigns.stream()
                .map(this::toResponseDTOWithCategories)
                .toList();
    }

    public CampaignResponseDTO getCampaignById(Long campaignId) {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        Campaign campaign = campaignServiceValidation.findVisibleCampaignByIdOrThrow(campaignId, callerId);

        return toResponseDTOWithCategories(campaign);
    }

    public List<CampaignResponseDTO> getCampaignsByAuthenticatedUser() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        List<Campaign> campaigns = campaignRepository.findByUserId(userId);

        return campaigns.stream()
                .map(this::toResponseDTOWithCategories)
                .toList();
    }

    private CampaignResponseDTO toResponseDTOWithCategories(Campaign campaign) {
        List<CampaignCategory> campaignCategories = campaignCategoryRepository.findByCampaignId(campaign.getId());
        return CampaignMapper.toResponseDTO(campaign, campaignCategories);
    }

    public List<CampaignResponseDTO> getCampaignsByStatus(String status) {
        CampaignStatus campaignStatus = campaignServiceValidation.parseStatus(status);

        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        List<Campaign> campaigns = campaignAuthorizationService.isAdmin(callerId)
                ? campaignRepository.findByGoalStatusWithActiveOwner(campaignStatus)
                : campaignRepository.findByGoalStatusVisibleTo(campaignStatus, callerId);

        return campaigns.stream()
                .map(this::toResponseDTOWithCategories)
                .toList();
    }

    public List<CampaignResponseDTO> getCampaignsByCategoryId(Long categoryId) {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        List<Campaign> campaigns = campaignAuthorizationService.isAdmin(callerId)
                ? campaignRepository.findByCategoryIdWithActiveOwner(categoryId)
                : campaignRepository.findByCategoryIdVisibleTo(categoryId, callerId);

        return campaigns.stream()
                .map(this::toResponseDTOWithCategories)
                .toList();
    }

    @Transactional
    public CampaignResponseDTO createCampaign(@NotNull CreateCampaignRequestDTO dto) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        campaignServiceValidation.validateCategoryIds(dto.getCategoryIds());

        Campaign campaign = CampaignMapper.toEntity(dto, user);

        Campaign savedCampaign = campaignRepository.save(campaign);

        saveCampaignCategories(savedCampaign, dto.getCategoryIds());

        campaignStatusHistoryService.recordTransition(savedCampaign, null, CampaignStatus.ACTIVE, userId);

        return toResponseDTOWithCategories(savedCampaign);
    }

    private void saveCampaignCategories(Campaign campaign, List<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        List<CampaignCategory> campaignCategories = categories.stream()
                .map(category -> CampaignCategory.builder()
                        .campaign(campaign)
                        .category(category)
                        .build())
                .toList();
        campaignCategoryRepository.saveAll(campaignCategories);
    }

    @Transactional
    public CampaignResponseDTO updateCampaign(Long campaignId, UpdateCampaignRequestDTO dto) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        campaignAuthorizationService.validateOwnershipOrAdmin(campaignId, userId);

        Campaign campaign = campaignServiceValidation.findCampaignByIdOrThrow(campaignId);

        campaignServiceValidation.validateCategoryIds(dto.getCategoryIds());

        campaignServiceValidation.updateCampaignFields(campaign, dto);
        updateGoalAndRecalculateStatus(campaign, dto, userId);

        Campaign updatedCampaign = campaignRepository.save(campaign);

        campaignCategoryRepository.deleteByCampaignId(updatedCampaign.getId());
        saveCampaignCategories(updatedCampaign, dto.getCategoryIds());

        return toResponseDTOWithCategories(updatedCampaign);
    }

    /**
     * Applies the new amountGoal and, unless the goal is CLOSED, recalculates its status with the real amount
     * raised, recording the transition with the editor. A CLOSED goal keeps its status: /activate recalculates it.
     */
    private void updateGoalAndRecalculateStatus(Campaign campaign, UpdateCampaignRequestDTO dto, Long userId) {
        Goal goal = campaign.getGoal();
        if (goal == null || dto.getAmountGoal() == null) {
            campaignServiceValidation.updateGoalFields(goal, dto);
            return;
        }

        lockGoal(goal);
        CampaignStatus previousStatus = goal.getStatus();
        campaignServiceValidation.updateGoalFields(goal, dto);

        if (previousStatus == CampaignStatus.CLOSED) {
            return;
        }

        CampaignStatus newStatus = campaignServiceValidation.statusForAmounts(goal);
        if (newStatus != previousStatus) {
            goal.setStatus(newStatus);
            campaignStatusHistoryService.recordTransition(campaign, previousStatus, newStatus, userId);
        }
    }

    /**
     * Re-reads the goal row with a pessimistic write lock (UPDLOCK in SQL Server), replacing the in-memory
     * values with the current ones, so a status decision uses the real status and amount raised and does not
     * interleave with the atomic updates of a donation. Call it before modifying the goal: refresh discards
     * in-memory changes.
     */
    private void lockGoal(Goal goal) {
        entityManager.refresh(goal, LockModeType.PESSIMISTIC_WRITE);
    }

    @Transactional
    public void deleteCampaign(Long campaignId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        campaignAuthorizationService.validateOwnershipOrAdmin(campaignId, userId);

        Campaign campaign = campaignServiceValidation.findCampaignByIdOrThrow(campaignId);

        closeAndSoftDelete(campaign, userId);
    }

    /**
     * Closes the campaign goal (recording the transition) if it is not already closed, then soft deletes the campaign.
     * Shared by the owner/admin delete endpoint and the moderation content removal.
     */
    public void closeAndSoftDelete(Campaign campaign, Long changedByUserId) {
        Goal goal = campaign.getGoal();
        if (goal != null) {
            lockGoal(goal);
            if (goal.getStatus() != CampaignStatus.CLOSED) {
                CampaignStatus previousStatus = goal.getStatus();
                goal.setStatus(CampaignStatus.CLOSED);
                campaignStatusHistoryService.recordTransition(campaign, previousStatus, CampaignStatus.CLOSED, changedByUserId);
                // Flush before deleting: the delete cascades to the goal, and Hibernate skips
                // the pending status update of an entity scheduled for removal.
                campaignRepository.saveAndFlush(campaign);
            }
        }

        campaignRepository.delete(campaign);
    }

    /**
     * Closes every campaign of the given owner that is not already closed, recording each transition
     * with the owner as changedBy. Does not delete them. Used when the owner deletes their account.
     */
    @Transactional
    public void closeAllByOwner(Long userId) {
        List<Campaign> campaignsToClose = new ArrayList<>();

        for (Campaign campaign : campaignRepository.findByUserId(userId)) {
            Goal goal = campaign.getGoal();
            if (goal == null) {
                continue;
            }
            lockGoal(goal);
            if (goal.getStatus() == CampaignStatus.CLOSED) {
                continue;
            }

            CampaignStatus previousStatus = goal.getStatus();
            goal.setStatus(CampaignStatus.CLOSED);
            campaignStatusHistoryService.recordTransition(campaign, previousStatus, CampaignStatus.CLOSED, userId);
            campaignsToClose.add(campaign);
        }

        campaignRepository.saveAll(campaignsToClose);
    }

    @Transactional
    public CampaignResponseDTO closeCampaign(Long campaignId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        Campaign campaign = campaignAuthorizationService.getCampaignIfAuthorized(campaignId, userId);

        Goal goal = campaign.getGoal();
        if (goal == null) {
            throw new MessageException("Campaign does not have a goal");
        }

        lockGoal(goal);
        if (goal.getStatus() == CampaignStatus.CLOSED) {
            throw new ConflictException("Campaign goal is already closed");
        }

        CampaignStatus previousStatus = goal.getStatus();
        goal.setStatus(CampaignStatus.CLOSED);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        campaignStatusHistoryService.recordTransition(updatedCampaign, previousStatus, CampaignStatus.CLOSED, userId);

        return toResponseDTOWithCategories(updatedCampaign);
    }

    @Transactional
    public CampaignResponseDTO activateCampaign(Long campaignId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        Campaign campaign = campaignAuthorizationService.getCampaignIfAuthorized(campaignId, userId);

        Goal goal = campaign.getGoal();
        if (goal == null) {
            throw new MessageException("Campaign does not have a goal");
        }

        lockGoal(goal);
        if (goal.getStatus() != CampaignStatus.CLOSED) {
            throw new MessageException("Campaign is not closed");
        }

        CampaignStatus newStatus = campaignServiceValidation.statusForAmounts(goal);
        goal.setStatus(newStatus);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        campaignStatusHistoryService.recordTransition(updatedCampaign, CampaignStatus.CLOSED, newStatus, userId);

        return toResponseDTOWithCategories(updatedCampaign);
    }

    public List<CampaignStatusHistoryResponseDTO> getCampaignStatusHistory(Long campaignId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        campaignAuthorizationService.validateOwnershipOrAdmin(campaignId, userId);

        campaignServiceValidation.findCampaignByIdOrThrow(campaignId);

        return campaignStatusHistoryRepository.findByCampaignIdOrderByChangedAtAsc(campaignId).stream()
                .map(CampaignStatusHistoryMapper::toResponseDTO)
                .toList();
    }

}

