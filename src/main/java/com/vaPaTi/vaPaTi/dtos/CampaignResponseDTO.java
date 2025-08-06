package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CampaignResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Double amountGoal;
    private Double amountRaised;
}

