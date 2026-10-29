package com.vaPaTi.vaPaTi.mapper.userblock;

import com.vaPaTi.vaPaTi.dtos.userblock.BlockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UnblockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UserBlockResponseDTO;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.entity.userblock.UserBlock;
import org.springframework.stereotype.Component;

@Component
public class UserBlockMapper {

    /**
     * Creates the block entity from the two users. createdAt stays null: @PrePersist assigns it
     */
    public UserBlock toEntity(User blocker, User blockedUser) {
        return UserBlock.builder()
                .blocker(blocker)
                .blockedUser(blockedUser)
                .build();
    }

    /**
     * Maps a block to the DTO of the blocked user
     */
    public UserBlockResponseDTO toDto(UserBlock block) {
        User blockedUser = block.getBlockedUser();
        UserInfo userInfo = blockedUser.getUserInfo();

        return new UserBlockResponseDTO(
                blockedUser.getId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getUserName(),
                userInfo.getProfilePicture(),
                block.getCreatedAt()
        );
    }

    /**
     * Creates a BlockResponseDTO for successful block operation
     */
    public BlockResponseDTO toBlockResponseDto(UserBlock block, String message) {
        return new BlockResponseDTO(message, true, toDto(block));
    }

    /**
     * Creates an UnblockResponseDTO for successful unblock operation
     */
    public UnblockResponseDTO toUnblockResponseDto(Long unblockedUserId, String message) {
        return new UnblockResponseDTO(message, true, unblockedUserId);
    }
}
