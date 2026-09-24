package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import com.vaPaTi.vaPaTi.entity.Goal;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("CampaignMapper Tests")
class CampaignMapperTest {

    @Test
    @DisplayName("toEntity should create the goal with amountRaised 0 and ACTIVE")
    void toEntity_ShouldCreateGoalWithZeroRaisedAndActive() {
        // Given
        CreateCampaignRequestDTO dto = new CreateCampaignRequestDTO(
                "Test Campaign", "Test Description", new BigDecimal("100.00"), List.of(1L));
        User user = new User();
        user.setId(1L);

        // When
        Campaign campaign = CampaignMapper.toEntity(dto, user);

        // Then
        Goal goal = campaign.getGoal();
        assertThat(goal.getAmountGoal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(goal.getAmountRaised()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(goal.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should map userName from the campaign owner's UserInfo")
    void toResponseDTO_WithUserInfo_ShouldMapUserName() {
        // Given
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName("john_doe");

        User user = new User();
        user.setId(1L);
        user.setUserInfo(userInfo);

        Goal goal = new Goal();
        goal.setAmountGoal(new BigDecimal("1000.0"));
        goal.setAmountRaised(new BigDecimal("0.0"));
        goal.setStatus(CampaignStatus.ACTIVE);

        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setName("Test Campaign");
        campaign.setDescription("Test Description");
        campaign.setUser(user);
        campaign.setGoal(goal);

        // When
        CampaignResponseDTO result = CampaignMapper.toResponseDTO(campaign, List.of());

        // Then
        assertThat(result.getUserName()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Should leave userName null when campaign has no user")
    void toResponseDTO_WithNullUser_ShouldLeaveUserNameNull() {
        // Given
        Goal goal = new Goal();
        goal.setAmountGoal(new BigDecimal("1000.0"));
        goal.setAmountRaised(new BigDecimal("0.0"));
        goal.setStatus(CampaignStatus.ACTIVE);

        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setName("Test Campaign");
        campaign.setDescription("Test Description");
        campaign.setUser(null);
        campaign.setGoal(goal);

        // When & Then
        assertThatCode(() -> {
            CampaignResponseDTO result = CampaignMapper.toResponseDTO(campaign, List.of());
            assertThat(result.getUserName()).isNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should leave userName null when campaign owner has no UserInfo")
    void toResponseDTO_WithNullUserInfo_ShouldLeaveUserNameNull() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setUserInfo(null);

        Goal goal = new Goal();
        goal.setAmountGoal(new BigDecimal("1000.0"));
        goal.setAmountRaised(new BigDecimal("0.0"));
        goal.setStatus(CampaignStatus.ACTIVE);

        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setName("Test Campaign");
        campaign.setDescription("Test Description");
        campaign.setUser(user);
        campaign.setGoal(goal);

        // When & Then
        assertThatCode(() -> {
            CampaignResponseDTO result = CampaignMapper.toResponseDTO(campaign, List.of());
            assertThat(result.getUserName()).isNull();
        }).doesNotThrowAnyException();
    }
}
