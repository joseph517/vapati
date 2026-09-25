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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private static final int CATEGORY_QUERY_CHUNK_SIZE = 1000;

    private final CampaignRepository campaignRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final CampaignServiceValidation campaignServiceValidation;
    private final CampaignAuthorizationService campaignAuthorizationService;
    private final CampaignCategoryRepository campaignCategoryRepository;
    private final CampaignStatusHistoryService campaignStatusHistoryService;
    private final CampaignStatusHistoryRepository campaignStatusHistoryRepository;
    private final EntityManager entityManager;

    // Public listings: admins see every campaign, everyone else (anonymous included) only their own CLOSED ones
    @Transactional
    public List<CampaignResponseDTO> getAllCampaigns() {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        List<Campaign> campaigns = campaignAuthorizationService.isAdmin(callerId)
                ? campaignRepository.findAllWithActiveOwner()
                : campaignRepository.findAllVisibleTo(callerId);

        return toResponseDTOsWithCategories(campaigns);
    }

    public CampaignResponseDTO getCampaignById(Long campaignId) {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        Campaign campaign = campaignServiceValidation.findVisibleCampaignByIdOrThrow(campaignId, callerId);

        return toResponseDTOWithCategories(campaign);
    }

    @Transactional
    public List<CampaignResponseDTO> getCampaignsByAuthenticatedUser() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        List<Campaign> campaigns = campaignRepository.findByUserIdWithDetails(userId);

        return toResponseDTOsWithCategories(campaigns);
    }

    private CampaignResponseDTO toResponseDTOWithCategories(Campaign campaign) {
        List<CampaignCategory> campaignCategories = campaignCategoryRepository.findByCampaignId(campaign.getId());
        return CampaignMapper.toResponseDTO(campaign, campaignCategories);
    }

    // Listings: the categories of every campaign in one query per chunk instead of one per campaign. Keeps the input order
    private List<CampaignResponseDTO> toResponseDTOsWithCategories(List<Campaign> campaigns) {
        // SQL Server rejects IN ()
        if (campaigns.isEmpty()) {
            return List.of();
        }

        List<Long> campaignIds = campaigns.stream().map(Campaign::getId).toList();
        Map<Long, List<CampaignCategory>> categoriesByCampaignId = new HashMap<>();
        // SQL Server accepts up to 2100 parameters per query
        for (int from = 0; from < campaignIds.size(); from += CATEGORY_QUERY_CHUNK_SIZE) {
            List<Long> chunk = campaignIds.subList(from, Math.min(from + CATEGORY_QUERY_CHUNK_SIZE, campaignIds.size()));
            for (CampaignCategory campaignCategory : campaignCategoryRepository.findByCampaignIdIn(chunk)) {
                categoriesByCampaignId
                        .computeIfAbsent(campaignCategory.getCampaign().getId(), id -> new ArrayList<>())
                        .add(campaignCategory);
            }
        }

        return campaigns.stream()
                .map(campaign -> CampaignMapper.toResponseDTO(campaign,
                        categoriesByCampaignId.getOrDefault(campaign.getId(), List.of())))
                .toList();
    }

    @Transactional
    public List<CampaignResponseDTO> getCampaignsByStatus(String status) {
        CampaignStatus campaignStatus = campaignServiceValidation.parseStatus(status);

        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        List<Campaign> campaigns = campaignAuthorizationService.isAdmin(callerId)
                ? campaignRepository.findByGoalStatusWithActiveOwner(campaignStatus)
                : campaignRepository.findByGoalStatusVisibleTo(campaignStatus, callerId);

        return toResponseDTOsWithCategories(campaigns);
    }

    @Transactional
    public List<CampaignResponseDTO> getCampaignsByCategoryId(Long categoryId) {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        List<Campaign> campaigns = campaignAuthorizationService.isAdmin(callerId)
                ? campaignRepository.findByCategoryIdWithActiveOwner(categoryId)
                : campaignRepository.findByCategoryIdVisibleTo(categoryId, callerId);

        return toResponseDTOsWithCategories(campaigns);
    }

    @Transactional
    public CampaignResponseDTO createCampaign(@NotNull CreateCampaignRequestDTO dto) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<Category> categories = campaignServiceValidation.validateAndGetCategories(dto.getCategoryIds());

        Campaign campaign = CampaignMapper.toEntity(dto, user);

        Campaign savedCampaign = campaignRepository.save(campaign);

        saveCampaignCategories(savedCampaign, categories);

        campaignStatusHistoryService.recordTransition(savedCampaign, null, CampaignStatus.ACTIVE, userId);

        return toResponseDTOWithCategories(savedCampaign);
    }

    // The categories come from validateAndGetCategories, so they aren't queried again
    private void saveCampaignCategories(Campaign campaign, List<Category> categories) {
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

        List<Category> categories = campaignServiceValidation.validateAndGetCategories(dto.getCategoryIds());

        campaignServiceValidation.updateCampaignFields(campaign, dto);
        updateGoalAndRecalculateStatus(campaign, dto, userId);

        Campaign updatedCampaign = campaignRepository.save(campaign);

        campaignCategoryRepository.deleteByCampaignId(updatedCampaign.getId());
        saveCampaignCategories(updatedCampaign, categories);

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

