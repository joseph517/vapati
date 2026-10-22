package com.vaPaTi.vaPaTi.service.campaign;

import com.vaPaTi.vaPaTi.AbstractIntegrationTest;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.campaign.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.campaign.Goal;
import com.vaPaTi.vaPaTi.entity.user.Role;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.repository.campaign.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.user.RoleRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignValidationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs closeAndSoftDelete against a real persistence context, because the failure it guards against
 * (a managed status history entry referencing the removed campaign at flush time) only shows up in Hibernate.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CampaignService.class, CampaignStatusHistoryService.class})
@DisplayName("CampaignService closeAndSoftDelete integration Tests")
class CampaignServiceCloseAndSoftDeleteIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;
    @MockitoBean
    private CampaignValidationService campaignValidationService;
    @MockitoBean
    private CampaignAuthorizationService campaignAuthorizationService;

    @Autowired
    private CampaignService campaignService;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private EntityManager entityManager;

    private User owner;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Role role = roleRepository.save(Role.builder().name("ROLE_USER_" + suffix).build());

        owner = User.builder()
                .verified(true)
                .role(role)
                .build();
        // The setter links both sides, which the builder does not
        owner.setUserInfo(UserInfo.builder()
                .firstName("Owner")
                .lastName("Test")
                .email("owner_" + suffix + "@example.com")
                .userName("owner_" + suffix)
                .password("securePassword123")
                .phone("1234567890")
                .description("Campaign owner")
                .build());
        owner = userRepository.save(owner);
    }

    private Long createCampaign(CampaignStatus status) {
        Goal goal = Goal.builder()
                .amountGoal(new BigDecimal("1000.00"))
                .amountRaised(BigDecimal.ZERO)
                .status(status)
                .build();
        Campaign campaign = Campaign.builder()
                .user(owner)
                .name("Campaign")
                .description("Campaign to delete")
                .goal(goal)
                .build();
        Long id = campaignRepository.saveAndFlush(campaign).getId();
        entityManager.clear();
        return id;
    }

    private Object singleValue(String sql, Long id) {
        return entityManager.createNativeQuery(sql).setParameter(1, id).getSingleResult();
    }

    @Test
    @DisplayName("Should close an ACTIVE campaign, record the transition and soft delete it, flushing without error")
    void closeAndSoftDelete_WithActiveGoal_ShouldFlushAndPersistEverything() {
        // Given
        Long campaignId = createCampaign(CampaignStatus.ACTIVE);
        Campaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        Long goalId = campaign.getGoal().getId();

        // When: the flush runs the same checks as the commit of the delete endpoint
        campaignService.closeAndSoftDelete(campaign, owner.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(campaignRepository.findById(campaignId)).isEmpty();
        assertThat(singleValue("SELECT deleted_at FROM campaign WHERE id = ?1", campaignId)).isNotNull();
        assertThat(singleValue("SELECT status FROM goal WHERE id = ?1", goalId)).isEqualTo("CLOSED");
        assertThat(singleValue("SELECT deleted_at FROM goal WHERE id = ?1", goalId)).isNotNull();
        assertThat(singleValue(
                "SELECT CONCAT(previous_status, '->', new_status, ':', changed_by) FROM campaign_status_history WHERE campaign_id = ?1",
                campaignId))
                .isEqualTo("ACTIVE->CLOSED:" + owner.getId());
    }

    @Test
    @DisplayName("Should soft delete an already CLOSED campaign without recording a transition")
    void closeAndSoftDelete_WithClosedGoal_ShouldOnlySoftDelete() {
        // Given
        Long campaignId = createCampaign(CampaignStatus.CLOSED);
        Campaign campaign = campaignRepository.findById(campaignId).orElseThrow();

        // When
        campaignService.closeAndSoftDelete(campaign, owner.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(singleValue("SELECT deleted_at FROM campaign WHERE id = ?1", campaignId)).isNotNull();
        assertThat(singleValue("SELECT COUNT(*) FROM campaign_status_history WHERE campaign_id = ?1", campaignId))
                .isEqualTo(0);
    }
}
