package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignAuthorizationService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String UNAUTHORIZED_MESSAGE = "You are not authorized to perform this action";
    private static final String CAMPAIGN_NOT_FOUND = "Campaign not found with id: ";

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

    public void validateOwnershipOrAdmin(Long campaignId, Long userId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new MessageException(CAMPAIGN_NOT_FOUND + campaignId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageException("User not found with id: " + userId));

        boolean isOwner = campaign.getUser().getId().equals(userId);
        boolean isAdmin = user.getRole() != null && ADMIN_ROLE.equals(user.getRole().getName());

        if (!isOwner && !isAdmin) {
            throw new MessageException(UNAUTHORIZED_MESSAGE);
        }
    }

    public boolean canCloseCampaign(Long campaignId, Long userId) {
        try {
            validateOwnershipOrAdmin(campaignId, userId);
            return true;
        } catch (MessageException e) {
            return false;
        }
    }

    public Campaign getCampaignIfAuthorized(Long campaignId, Long userId) {
        validateOwnershipOrAdmin(campaignId, userId);
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new MessageException(CAMPAIGN_NOT_FOUND + campaignId));
    }
}
