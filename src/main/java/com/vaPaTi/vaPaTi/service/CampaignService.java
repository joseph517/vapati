package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.CampaignMapper;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
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

    public List<CampaignResponseDTO> getAllCampaigns() {
        List<Campaign> campaigns = campaignRepository.findAll();

        return campaigns.stream()
                .map(CampaignMapper::toResponseDTO)
                .toList();
    }

    public List<CampaignResponseDTO> getCampaignsByAuthenticatedUser() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        List<Campaign> campaigns = campaignRepository.findByUserId(userId);

        return campaigns.stream()
                .map(CampaignMapper::toResponseDTO)
                .toList();
    }

    public CampaignResponseDTO createCampaign(@NotNull CreateCampaignRequestDTO dto) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Double amountRaised = dto.getAmountRaised() != null ? dto.getAmountRaised() : 0;
        dto.setAmountRaised(amountRaised);

        Campaign campaign = CampaignMapper.toEntity(dto, user);

        Campaign savedCampaign = campaignRepository.save(campaign);

        return CampaignMapper.toResponseDTO(savedCampaign);
    }

    @Transactional
    public CampaignResponseDTO updateCampaign(Long campaignId, UpdateCampaignRequestDTO dto) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        campaignAuthorizationService.validateOwnershipOrAdmin(campaignId, userId);

        Campaign campaign = campaignServiceValidation.findCampaignByIdOrThrow(campaignId);

        campaignServiceValidation.updateCampaignFields(campaign, dto);
        campaignServiceValidation.updateGoalFields(campaign.getGoal(), dto);

        Campaign updatedCampaign = campaignRepository.save(campaign);

        return CampaignMapper.toResponseDTO(updatedCampaign);
    }

    public void deleteCampaign(Long campaignId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        campaignAuthorizationService.validateOwnershipOrAdmin(campaignId, userId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        // When deleting, also deactivate the goal
        if (campaign.getGoal() != null) {
            campaign.getGoal().setActive(false);
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

        if (Boolean.FALSE.equals(goal.getActive())) {
            throw new MessageException("Campaign goal is already closed");
        }

        goal.setActive(false);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        return CampaignMapper.toResponseDTO(updatedCampaign);
    }

}

