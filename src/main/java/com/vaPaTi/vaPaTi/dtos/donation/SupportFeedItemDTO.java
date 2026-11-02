package com.vaPaTi.vaPaTi.dtos.donation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportFeedItemDTO {

    private Long donationId;
    private Long donorUserId;
    private String donorUserName;
    private Long campaignId;
    private String campaignName;
    private Boolean campaignHidden;
    private String message;
    private String thanksMessage;
    private LocalDateTime thankedAt;
    private LocalDateTime createdAt;
}
