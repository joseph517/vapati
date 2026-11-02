package com.vaPaTi.vaPaTi.dtos.comment;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentDTO {

    @Size(max = 280, message = "Text must be at most 280 characters")
    private String text;
}
