package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.SupporterResponseDTO;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationMapper;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.donation.DonationValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    // One row per donor with the aggregates of their COMPLETED donations to the creator's campaigns (deleted ones
    // included). Everyone sees the donor. Only the creator and admins see the aggregates.
    // The creator checks run before the sort check: a hidden profile gives 404, never the 400 of the sort
    @Transactional
    public Page<SupporterResponseDTO> getSupporters(Long userId, Pageable pageable) {
        if (userId == null || userId <= 0) {
            throw new MessageException("ID must be a positive number");
        }

        User creator = userValidationService.getUserById(userId);

        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        boolean fullView = userId.equals(callerId) || userValidationService.isAdmin(callerId);
        if (!fullView) {
            userValidationService.validateProfileNotBlocked(creator);
        }

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
}
