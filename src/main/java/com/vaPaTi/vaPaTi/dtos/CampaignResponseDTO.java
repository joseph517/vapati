package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CampaignResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Double amountGoal;
    private Double amountRaised;
    private Long userId;
    private List<CategoryDTO> categories;
    private CampaignStatus status;
}

