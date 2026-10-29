package com.vaPaTi.vaPaTi.service.userblock;

import com.vaPaTi.vaPaTi.dtos.userblock.BlockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UnblockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UserBlockResponseDTO;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.userblock.UserBlock;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.userblock.UserBlockMapper;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.repository.userblock.UserBlockRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.follower.FollowerService;
import com.vaPaTi.vaPaTi.validation.userblock.UserBlockValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// The blocker is always the caller from the token; the userId of each method is the other user.
// An ADMIN has no exception: blocks and gets blocked like anyone else
@Service
@RequiredArgsConstructor
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserBlockValidationService userBlockValidationService;
    private final FollowerService followerService;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final UserBlockMapper userBlockMapper;

    /**
     * Blocks a user and removes the follows in both directions between both users.
     * @param userToBlockId ID of the user to block
     * @return a BlockResponseDTO with the blocked user and a success message
     * @throws MessageException if the current user is trying to block themselves
     * @throws ResourceNotFoundException if the user to block does not exist, is deleted, banned or suspended
     * @throws ConflictException if the current user already blocks the user
     */
    @Transactional
    public BlockResponseDTO blockUser(Long userToBlockId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        userBlockValidationService.validateNotSelfBlock(userId, userToBlockId);
        User userToBlock = userBlockValidationService.validateAndGetUserToBlock(userToBlockId);
        userBlockValidationService.validateNotAlreadyBlocked(userId, userToBlockId);

        // getReferenceById returns a proxy: no SELECT of the caller just to link it, the token already proved it exists.
        // The INSERT goes before removing the follows: with IDENTITY it runs right away, so a simultaneous duplicate
        // fails on the unique constraint before touching followers, and the rollback leaves the follows as they were
        UserBlock savedBlock = userBlockRepository.save(
                userBlockMapper.toEntity(userRepository.getReferenceById(userId), userToBlock));
        followerService.removeFollowsBetween(userId, userToBlockId);

        return userBlockMapper.toBlockResponseDto(
                savedBlock,
                "Successfully blocked " + userToBlock.getUserInfo().getUserName()
        );
    }

    /**
     * Unblocks a user by deleting the block of the current user. The follows removed by the block are not restored.
     * @param userToUnblockId ID of the user to unblock
     * @return an UnblockResponseDTO with the ID of the unblocked user and a success message
     * @throws MessageException if the current user is trying to unblock themselves
     * @throws ResourceNotFoundException if the current user does not block the user
     */
    @Transactional
    public UnblockResponseDTO unblockUser(Long userToUnblockId) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        userBlockValidationService.validateNotSelfUnblock(userId, userToUnblockId);
        UserBlock block = userBlockValidationService.validateAndGetBlock(userId, userToUnblockId);

        userBlockRepository.delete(block);

        // No userName: the unblocked user may be deleted or sanctioned
        return userBlockMapper.toUnblockResponseDto(userToUnblockId, "User successfully unblocked");
    }

    /**
     * Checks if the current user blocks another user. Never answers 404.
     * @param otherUserId ID of the other user
     * @return true if the current user blocks the other user
     */
    public boolean isBlocking(Long otherUserId) {
        return userBlockValidationService.isBlocking(authenticatedUserService.getAuthenticatedUserId(), otherUserId);
    }

    /**
     * Gets the blocks of the current user, without deleted, banned or currently suspended blocked users.
     * id DESC breaks ties between blocks with the same createdAt, so the pages don't overlap.
     * @param pageable page, size and sort already validated
     * @return the page of blocked users
     */
    @Transactional
    public Page<UserBlockResponseDTO> getMyBlocks(Pageable pageable) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id")));

        return userBlockRepository.findVisibleByBlockerId(userId, LocalDateTime.now(), sorted)
                .map(userBlockMapper::toDto);
    }
}
