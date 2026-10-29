package com.vaPaTi.vaPaTi.repository.follower;

import com.vaPaTi.vaPaTi.AbstractIntegrationTest;
import com.vaPaTi.vaPaTi.entity.follower.Follower;
import com.vaPaTi.vaPaTi.entity.user.Role;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.repository.user.RoleRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// deleteFollowsBetween against SQL Server: the unit tests mock it, so the JPQL of the bulk delete is checked here
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FollowerRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private FollowerRepository followerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        userA = createTestUser("follow.a");
        userB = createTestUser("follow.b");
        userC = createTestUser("follow.c");
    }

    @Test
    @DisplayName("deleteFollowsBetween should delete the follows in both directions and keep the others")
    void deleteFollowsBetween_BothDirections_ShouldDeleteOnlyThose() {
        follow(userB, userA);
        follow(userA, userB);
        follow(userC, userA);
        follow(userB, userC);

        int deleted = followerRepository.deleteFollowsBetween(userA.getId(), userB.getId());

        assertThat(deleted).isEqualTo(2);
        assertThat(followerRepository.existsByUserAndFollower(userB, userA)).isFalse();
        assertThat(followerRepository.existsByUserAndFollower(userA, userB)).isFalse();
        assertThat(followerRepository.existsByUserAndFollower(userC, userA)).isTrue();
        assertThat(followerRepository.existsByUserAndFollower(userB, userC)).isTrue();
    }

    @Test
    @DisplayName("deleteFollowsBetween should delete a one-way follow with the ids in either order")
    void deleteFollowsBetween_OneWay_ShouldDeleteWithIdsInEitherOrder() {
        follow(userB, userA);

        int deleted = followerRepository.deleteFollowsBetween(userB.getId(), userA.getId());

        assertThat(deleted).isEqualTo(1);
        assertThat(followerRepository.existsByUserAndFollower(userB, userA)).isFalse();
    }

    @Test
    @DisplayName("deleteFollowsBetween should return 0 when the users don't follow each other")
    void deleteFollowsBetween_NoFollows_ShouldReturnZero() {
        follow(userC, userA);

        assertThat(followerRepository.deleteFollowsBetween(userA.getId(), userB.getId())).isZero();
        assertThat(followerRepository.existsByUserAndFollower(userC, userA)).isTrue();
    }

    // follower follows user
    private void follow(User user, User follower) {
        Follower relation = new Follower();
        relation.setUser(user);
        relation.setFollower(follower);
        relation.setCreatedAt(LocalDateTime.now());
        followerRepository.save(relation);
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
