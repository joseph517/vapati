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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

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

    public List<CampaignResponseDTO> getAllCampaigns() {
        List<Campaign> campaigns = campaignRepository.findAllWithActiveOwner();

        return campaigns.stream()
                .map(this::toResponseDTOWithCategories)
                .toList();
    }

    public CampaignResponseDTO getCampaignById(Long campaignId) {
        Campaign campaign = campaignServiceValidation.findCampaignByIdOrThrow(campaignId);

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

        List<Campaign> campaigns = campaignRepository.findByGoalStatusWithActiveOwner(campaignStatus);

        return campaigns.stream()
                .map(this::toResponseDTOWithCategories)
                .toList();
    }

    public List<CampaignResponseDTO> getCampaignsByCategoryId(Long categoryId) {
        List<Long> campaignIds = campaignCategoryRepository.findByCategoryId(categoryId).stream()
                .map(campaignCategory -> campaignCategory.getCampaign().getId())
                .toList();

        List<Campaign> campaigns = campaignRepository.findAllById(campaignIds);

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

        Double amountRaised = dto.getAmountRaised() != null ? dto.getAmountRaised() : 0;
        dto.setAmountRaised(amountRaised);

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
        campaignServiceValidation.updateGoalFields(campaign.getGoal(), dto);

        Campaign updatedCampaign = campaignRepository.save(campaign);

        campaignCategoryRepository.deleteByCampaignId(updatedCampaign.getId());
        saveCampaignCategories(updatedCampaign, dto.getCategoryIds());

        return toResponseDTOWithCategories(updatedCampaign);
    }

    public void deleteCampaign(Long campaignId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        campaignAuthorizationService.validateOwnershipOrAdmin(campaignId, userId);

        Campaign campaign = campaignServiceValidation.findCampaignByIdOrThrow(campaignId);

        // When deleting, also deactivate the goal
        if (campaign.getGoal() != null && campaign.getGoal().getStatus() != CampaignStatus.CLOSED) {
            CampaignStatus previousStatus = campaign.getGoal().getStatus();
            campaign.getGoal().setStatus(CampaignStatus.CLOSED);
            campaignStatusHistoryService.recordTransition(campaign, previousStatus, CampaignStatus.CLOSED, userId);
        }

        campaignRepository.delete(campaign);
    }

    @Transactional
    public CampaignResponseDTO closeCampaign(Long campaignId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        Campaign campaign = campaignAuthorizationService.getCampaignIfAuthorized(campaignId, userId);

        Goal goal = campaign.getGoal();
        if (goal == null) {
            throw new MessageException("Campaign does not have a goal");
        }

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

        if (goal.getStatus() != CampaignStatus.CLOSED) {
            throw new MessageException("Campaign is not closed");
        }

        CampaignStatus newStatus = goal.getAmountRaised() >= goal.getAmountGoal() ? CampaignStatus.COMPLETED : CampaignStatus.ACTIVE;
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

