package com.vaPaTi.vaPaTi.dtos;

import com.vaPaTi.vaPaTi.entity.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CampaignResponseDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal amountGoal;
    private BigDecimal amountRaised;
    private Long userId;
    private List<CategoryDTO> categories;
    private CampaignStatus status;
    private String userName;
}

