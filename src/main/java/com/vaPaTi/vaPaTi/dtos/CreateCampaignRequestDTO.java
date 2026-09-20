package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCampaignRequestDTO {

    private String name;
    private String description;
    private Double amountGoal;
    private Double amountRaised;

    @NotEmpty
    @Size(min = 1, max = 5)
    private List<Long> categoryIds;

}
