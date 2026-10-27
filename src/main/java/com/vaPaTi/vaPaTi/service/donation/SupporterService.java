package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.SupportFeedItemDTO;
import com.vaPaTi.vaPaTi.dtos.donation.SupporterResponseDTO;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationMapper;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
import com.vaPaTi.vaPaTi.validation.donation.DonationValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupporterService {

    private static final String DEFAULT_SORT_BY = "totalAmount";

    private final DonationRepository donationRepository;
    private final DonationValidationService donationValidationService;
    private final DonationMapper donationMapper;
    private final UserRepository userRepository;
    private final UserValidationService userValidationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final RoleValidationService roleValidationService;

    // One row per donor with the aggregates of their COMPLETED donations received by the creator, direct and to
    // their campaigns (deleted ones included). Everyone sees the donor. Only the creator and admins see the aggregates.
    // The creator checks run before the sort check: a hidden profile gives 404, never the 400 of the sort
    @Transactional
    public Page<SupporterResponseDTO> getSupporters(Long userId, Pageable pageable) {
        boolean fullView = validateCreatorAndGetPrivileged(userId);

        Sort.Order order = pageable.getSort().stream().findFirst().orElse(null);
        String sortBy = order != null ? order.getProperty() : DEFAULT_SORT_BY;
        boolean descending = order == null || order.isDescending();
        donationValidationService.validateSupporterSort(sortBy, fullView);

        List<DonationRepository.SupporterProjection> supporters = donationRepository.findSupportersByCreatorId(
                userId, sortBy, descending, pageable.getOffset(), pageable.getPageSize());
        long total = donationRepository.countSupportersByCreatorId(userId);
        if (supporters.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // A donor that doesn't come back is soft deleted: the mapper makes the row anonymous
        List<Long> donorIds = supporters.stream()
                .map(DonationRepository.SupporterProjection::getDonorUserId)
                .toList();
        Map<Long, User> donorsById = new HashMap<>();
        for (User donor : userRepository.findAllWithDetailsByIdIn(donorIds)) {
            donorsById.put(donor.getId(), donor);
        }

        List<SupporterResponseDTO> rows = supporters.stream()
                .map(row -> donationMapper.toSupporterDTO(row, donorsById.get(row.getDonorUserId())))
                .toList();
        if (!fullView) {
            rows.forEach(dto -> {
                dto.setTotalAmount(null);
                dto.setDonationsCount(null);
                dto.setLastDonationAt(null);
            });
        }
        return new PageImpl<>(rows, pageable, total);
    }

    // The COMPLETED donations received by the creator, direct and to their campaigns, one item per donation. The feed
    // is the same for everyone: being privileged only lets an admin see the feed of a banned or suspended creator.
    // id DESC breaks ties between donations with the same createdAt, so the pages don't overlap
    @Transactional
    public Page<SupportFeedItemDTO> getSupportFeed(Long userId, Pageable pageable) {
        validateCreatorAndGetPrivileged(userId);

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id")));

        return donationRepository.findCompletedByRecipientId(userId, sorted)
                .map(donation -> hideNonPublicData(donation, donationMapper.toSupportFeedItemDTO(donation)));
    }

    // The feed shows the public view to everyone, the creator and admins included: a campaign the public can't see
    // loses its id and name, and a private message or reply is left out. The private data stays in /received
    private SupportFeedItemDTO hideNonPublicData(Donation donation, SupportFeedItemDTO dto) {
        if (donation.getCampaignId() != null && !donationValidationService.isCampaignPublic(donation)) {
            dto.setCampaignId(null);
            dto.setCampaignName(null);
            dto.setCampaignHidden(true);
        }
        if (donation.isMessagePrivate()) {
            dto.setMessage(null);
        }
        if (donation.isThanksPrivate()) {
            dto.setThanksMessage(null);
            dto.setThankedAt(null);
        }
        return dto;
    }

    // The creator must exist. Only the creator and admins see a banned or suspended creator; the rest get a 404.
    // Returns whether the caller is the creator or an admin
    private boolean validateCreatorAndGetPrivileged(Long userId) {
        if (userId == null || userId <= 0) {
            throw new MessageException("ID must be a positive number");
        }

        User creator = userValidationService.getUserById(userId);

        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        boolean privileged = userId.equals(callerId) || roleValidationService.isAdmin(callerId);
        if (!privileged) {
            userValidationService.validateProfileNotSanctioned(creator);
        }
        return privileged;
    }
}
