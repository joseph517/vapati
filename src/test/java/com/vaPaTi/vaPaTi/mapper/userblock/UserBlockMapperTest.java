package com.vaPaTi.vaPaTi.mapper.userblock;

import com.vaPaTi.vaPaTi.dtos.userblock.BlockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UnblockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UserBlockResponseDTO;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.entity.userblock.UserBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserBlockMapper Tests")
class UserBlockMapperTest {

    private final UserBlockMapper userBlockMapper = new UserBlockMapper();

    private User blocker;
    private User blocked;

    @BeforeEach
    void setUp() {
        blocker = new User();
        blocker.setId(1L);

        UserInfo userInfo = new UserInfo();
        userInfo.setFirstName("Carla");
        userInfo.setLastName("Blocked");
        userInfo.setUserName("carla_blocked");
        userInfo.setProfilePicture("https://img/carla.png");

        blocked = new User();
        blocked.setId(2L);
        blocked.setUserInfo(userInfo);
    }

    @Test
    @DisplayName("toEntity should set blocker and blocked user and leave createdAt null")
    void toEntity_ShouldSetUsersAndLeaveCreatedAtNull() {
        UserBlock block = userBlockMapper.toEntity(blocker, blocked);

        assertThat(block.getBlocker()).isSameAs(blocker);
        assertThat(block.getBlockedUser()).isSameAs(blocked);
        assertThat(block.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("toDto should copy the blocked user fields and blockedAt")
    void toDto_ShouldCopyAllFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        UserBlock block = UserBlock.builder()
                .id(7L).blocker(blocker).blockedUser(blocked).createdAt(createdAt).build();

        UserBlockResponseDTO dto = userBlockMapper.toDto(block);

        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getFirstName()).isEqualTo("Carla");
        assertThat(dto.getLastName()).isEqualTo("Blocked");
        assertThat(dto.getUserName()).isEqualTo("carla_blocked");
        assertThat(dto.getProfilePicture()).isEqualTo("https://img/carla.png");
        assertThat(dto.getBlockedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("toBlockResponseDto should return success true, the message and the blocked user")
    void toBlockResponseDto_ShouldReturnSuccessWithMessage() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        UserBlock block = UserBlock.builder()
                .blocker(blocker).blockedUser(blocked).createdAt(createdAt).build();

        BlockResponseDTO dto = userBlockMapper.toBlockResponseDto(block, "Successfully blocked carla_blocked");

        assertThat(dto.isSuccess()).isTrue();
        assertThat(dto.getMessage()).isEqualTo("Successfully blocked carla_blocked");
        assertThat(dto.getBlockedUser().getId()).isEqualTo(2L);
        assertThat(dto.getBlockedUser().getBlockedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("toUnblockResponseDto should return success true, the message and the unblocked id")
    void toUnblockResponseDto_ShouldReturnSuccessWithMessage() {
        UnblockResponseDTO dto = userBlockMapper.toUnblockResponseDto(2L, "User successfully unblocked");

        assertThat(dto.isSuccess()).isTrue();
        assertThat(dto.getMessage()).isEqualTo("User successfully unblocked");
        assertThat(dto.getUnblockedUserId()).isEqualTo(2L);
    }
}
