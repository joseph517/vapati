package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.repository.FollowerRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowerValidation {

    private final UserRepository userRepository;
    private final FollowerRepository followerRepository;

    /**
     * Check if current user follows another user
     * @param currentUserId ID of the current user
     * @param otherUserId ID of the other user
     * @return true if current user follows the other user
     */
    public boolean isFollowing(@NotNull Long currentUserId, Long otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            return false; // User cannot follow themselves
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found with ID: " + currentUserId));

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new EntityNotFoundException("Other user not found with ID: " + otherUserId));

        return followerRepository.existsByUserAndFollower(otherUser, currentUser);
    }
}
