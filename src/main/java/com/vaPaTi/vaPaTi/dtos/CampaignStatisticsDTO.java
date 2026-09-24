package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatisticsDTO {

    private Long campaignId;
    private String campaignName;
    private BigDecimal amountGoal;
    private BigDecimal amountRaised;
    private BigDecimal percentageReached;
    private Boolean isGoalReached;
    private CampaignStatus status;
    private Long totalDonors;
    private BigDecimal averageDonation;
    private Long totalDonations;
}
