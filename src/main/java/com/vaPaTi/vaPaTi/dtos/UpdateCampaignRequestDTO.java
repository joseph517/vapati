package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCampaignRequestDTO {

    @Size(max = 255)
    private String name;
    @Size(max = 255)
    private String description;
    private BigDecimal amountGoal;

    @NotEmpty
    @Size(min = 1, max = 5)
    private List<Long> categoryIds;
}

