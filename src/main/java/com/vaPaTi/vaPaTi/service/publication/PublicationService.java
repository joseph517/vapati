package com.vaPaTi.vaPaTi.service.publication;

import com.vaPaTi.vaPaTi.dtos.publication.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.publication.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.mapper.publication.PublicationMapper;
import com.vaPaTi.vaPaTi.repository.publication.PublicationLikeRepository;
import com.vaPaTi.vaPaTi.repository.publication.PublicationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private final PublicationRepository publicationRepository;
    private final PublicationMapper publicationMapper;
    private final AuthenticatedUserService authenticatedUserService;
    private final PublicationValidationService publicationValidationService;
    private final PublicationLikeRepository publicationLikeRepository;

    public PublicationResponseDTO createPublication(Long campaignId, CreatePublicationDTO dto) {
        Long callerId = authenticatedUserService.getAuthenticatedUserId();
        Campaign campaign = publicationValidationService.validateAndGetPublishableCampaign(campaignId, callerId);

        Publication publication = publicationMapper.toEntity(dto, campaign);
        publication = publicationRepository.save(publication);

        return publicationMapper.toDTO(publication);
    }

    // Public: callerId is null for an anonymous caller, who doesn't see a CLOSED campaign
    public List<PublicationResponseDTO> getPublicationsByCampaignId(Long campaignId) {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        publicationValidationService.validateAndGetReadableCampaign(campaignId, callerId);

        List<Publication> publications = publicationRepository.findAllByCampaignId(campaignId);
        if (publications.isEmpty()) {
            return List.of();
        }

        // Publications without visible likes don't come back from the count: they default to 0
        Map<Long, Long> likesByPublication = publicationLikeRepository
                .countVisibleByCampaignIdGroupedByPublication(campaignId, LocalDateTime.now())
                .stream()
                .collect(Collectors.toMap(
                        PublicationLikeRepository.PublicationLikeCountProjection::getPublicationId,
                        PublicationLikeRepository.PublicationLikeCountProjection::getLikesCount));
        // An anonymous caller has no likes
        Set<Long> likedByCaller = callerId == null
                ? Set.of()
                : new HashSet<>(publicationLikeRepository
                        .findLikedPublicationIdsByCampaignIdAndUserId(campaignId, callerId));

        return publications.stream()
                .map(p -> publicationMapper.toDTO(p,
                        likesByPublication.getOrDefault(p.getId(), 0L),
                        likedByCaller.contains(p.getId())))
                .toList();
    }

    public void deletePublication(Long publicationId) {
        Long callerId = authenticatedUserService.getAuthenticatedUserId();
        Publication publication = publicationValidationService.validateAndGetOwnedPublication(publicationId, callerId);

        publicationRepository.delete(publication);
    }

}
