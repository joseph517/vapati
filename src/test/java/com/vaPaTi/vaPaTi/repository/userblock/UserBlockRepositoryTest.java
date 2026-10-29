package com.vaPaTi.vaPaTi.repository.userblock;

import com.vaPaTi.vaPaTi.AbstractIntegrationTest;
import com.vaPaTi.vaPaTi.entity.user.Role;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.entity.userblock.UserBlock;
import com.vaPaTi.vaPaTi.repository.user.RoleRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// existsBlockBetween against SQL Server: the unit tests mock it, so the symmetry of its two branches is checked here
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserBlockRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserBlockRepository userBlockRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    private User blocker;
    private User blockedUser;
    private User thirdUser;

    @BeforeEach
    void setUp() {
        blocker = createTestUser("block.blocker");
        blockedUser = createTestUser("block.blocked");
        thirdUser = createTestUser("block.third");
        userBlockRepository.save(UserBlock.builder().blocker(blocker).blockedUser(blockedUser).build());
    }

    @Test
    @DisplayName("existsBlockBetween should find the block in the direction it was created")
    void existsBlockBetween_SameDirection_ShouldBeTrue() {
        assertThat(userBlockRepository.existsBlockBetween(blocker.getId(), blockedUser.getId())).isTrue();
    }

    @Test
    @DisplayName("existsBlockBetween should find the block with the ids in the other order")
    void existsBlockBetween_OtherDirection_ShouldBeTrue() {
        assertThat(userBlockRepository.existsBlockBetween(blockedUser.getId(), blocker.getId())).isTrue();
    }

    @Test
    @DisplayName("existsBlockBetween should be false between users without a block, in both orders")
    void existsBlockBetween_NoBlock_ShouldBeFalse() {
        assertThat(userBlockRepository.existsBlockBetween(blocker.getId(), thirdUser.getId())).isFalse();
        assertThat(userBlockRepository.existsBlockBetween(thirdUser.getId(), blockedUser.getId())).isFalse();
    }

    @Test
    @DisplayName("existsByBlockerIdAndBlockedUserId should only find the direction it was created")
    void existsByBlockerIdAndBlockedUserId_ShouldBeDirectional() {
        assertThat(userBlockRepository.existsByBlockerIdAndBlockedUserId(blocker.getId(), blockedUser.getId()))
                .isTrue();
        assertThat(userBlockRepository.existsByBlockerIdAndBlockedUserId(blockedUser.getId(), blocker.getId()))
                .isFalse();
    }

    private User createTestUser(String userName) {
        Role role = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User user = User.builder()
                .verified(true)
                .role(role)
                .build();

        user.setUserInfo(UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .email(userName + "@example.com")
                .userName(userName)
                .password("securePassword123" + UUID.randomUUID())
                .phone("12345" + UUID.randomUUID().toString().substring(0, 5))
                .description("Test user")
                .build());

        return userRepository.save(user);
    }
}
