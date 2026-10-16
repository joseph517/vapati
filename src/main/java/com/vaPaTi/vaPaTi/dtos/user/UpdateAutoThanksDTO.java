package com.vaPaTi.vaPaTi.dtos.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAutoThanksDTO {

    @Size(max = 280, message = "Auto thanks message must be at most 280 characters")
    private String autoThanksMessage;
}
