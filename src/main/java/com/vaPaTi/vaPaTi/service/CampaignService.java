package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.mapper.CampaignMapper;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.CampaignServiceValidation;
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

    public CampaignResponseDTO updateCampaign(Long campaignId, UpdateCampaignRequestDTO dto) {
        Campaign campaign = campaignServiceValidation.findCampaignByIdOrThrow(campaignId);

        campaignServiceValidation.updateCampaignFields(campaign, dto);
        campaignServiceValidation.updateGoalFields(campaign.getGoal(), dto);

        Campaign updatedCampaign = campaignRepository.save(campaign);

        return CampaignMapper.toResponseDTO(updatedCampaign);
    }

    public void deleteCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        campaignRepository.delete(campaign);
    }

}

