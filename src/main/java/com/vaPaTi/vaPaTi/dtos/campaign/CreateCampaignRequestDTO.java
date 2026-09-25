package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCampaignRequestDTO {

    @NotBlank
    @Size(max = 255)
    private String name;
    @NotNull
    @Size(max = 255)
    private String description;
    @NotNull
    @Positive
    @Digits(integer = 13, fraction = 2)
    private BigDecimal amountGoal;

    @NotEmpty
    @Size(min = 1, max = 5)
    private List<Long> categoryIds;

}
