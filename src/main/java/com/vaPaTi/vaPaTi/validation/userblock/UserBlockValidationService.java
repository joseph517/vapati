package com.vaPaTi.vaPaTi.validation.userblock;

import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.userblock.UserBlock;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.repository.userblock.UserBlockRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

// Depends only on its repository, UserRepository and AccountStatusValidationService, so follower, comment and
// later domains can inject it without cycles
@Component
@RequiredArgsConstructor
public class UserBlockValidationService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final AccountStatusValidationService accountStatusValidationService;

    /**
     * Validates that there is no block in either direction between two users. The message is the same in both
     * directions, so it does not reveal who blocked whom. With a null ID nothing is checked.
     * @param userId ID of the authenticated user
     * @param otherUserId ID of the other user
     * @throws ForbiddenActionException if either user blocks the other
     */
    public void validateNoBlockBetween(Long userId, Long otherUserId) {
        if (userId == null || otherUserId == null) {
            return;
        }
        if (userBlockRepository.existsBlockBetween(userId, otherUserId)) {
            throw new ForbiddenActionException("You cannot interact with this user");
        }
    }

    /**
     * Validates that the current user is not trying to block themselves.
     * @param currentUserId ID of the authenticated user
     * @param targetUserId ID of the user to block
     * @throws MessageException if both IDs are the same
     */
    public void validateNotSelfBlock(@NotNull Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new MessageException("Users cannot block themselves");
        }
    }

    /**
     * Validates that the current user is not trying to unblock themselves.
     * @param currentUserId ID of the authenticated user
     * @param targetUserId ID of the user to unblock
     * @throws MessageException if both IDs are the same
     */
    public void validateNotSelfUnblock(@NotNull Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new MessageException("Users cannot unblock themselves");
        }
    }

    /**
     * Loads the user to block. Deleted, banned and suspended users are not found, also for an ADMIN, so the
     * sanction is not revealed.
     * @param userId ID of the user to block
     * @return the user to block
     * @throws ResourceNotFoundException if the user does not exist or is banned or suspended
     */
    public User validateAndGetUserToBlock(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !accountStatusValidationService.isSanctioned(user))
                .orElseThrow(() -> new ResourceNotFoundException("User to block not found with ID: " + userId));
    }

    /**
     * Validates that the current user has not blocked the other user yet. A block in the opposite direction
     * does not count.
     * @param blockerId ID of the authenticated user
     * @param blockedUserId ID of the user to block
     * @throws ConflictException if the current user already blocks the other user
     */
    public void validateNotAlreadyBlocked(Long blockerId, Long blockedUserId) {
        if (userBlockRepository.existsByBlockerIdAndBlockedUserId(blockerId, blockedUserId)) {
            throw new ConflictException("You have already blocked this user");
        }
    }

    /**
     * Loads the block of the current user over the other user. Only the row is looked up, without loading or
     * validating the blocked user, so a deleted or sanctioned user can still be unblocked.
     * @param blockerId ID of the authenticated user
     * @param blockedUserId ID of the blocked user
     * @return the block
     * @throws ResourceNotFoundException if the current user does not block the other user
     */
    public UserBlock validateAndGetBlock(Long blockerId, Long blockedUserId) {
        return userBlockRepository.findByBlockerIdAndBlockedUserId(blockerId, blockedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Block not found"));
    }

    /**
     * Checks if the current user blocks another user. It never loads the other user nor answers 404, so it does
     * not reveal whether the user exists or is sanctioned.
     * @param currentUserId ID of the authenticated user
     * @param otherUserId ID of the other user
     * @return true if the current user blocks the other user; false with the same ID
     */
    public boolean isBlocking(@NotNull Long currentUserId, Long otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            return false;
        }
        return userBlockRepository.existsByBlockerIdAndBlockedUserId(currentUserId, otherUserId);
    }
}
