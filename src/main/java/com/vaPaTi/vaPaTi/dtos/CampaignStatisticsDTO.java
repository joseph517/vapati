package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatisticsDTO {

    private Long campaignId;
    private String campaignName;
    private Double amountGoal;
    private Double amountRaised;
    private Double percentageReached;
    private Boolean isGoalReached;
    private Boolean isActive;
    private LocalDateTime targetDate;
    private Long daysRemaining;
    private Long totalDonors;
    private Double averageDonation;
    private Long totalDonations;
}
