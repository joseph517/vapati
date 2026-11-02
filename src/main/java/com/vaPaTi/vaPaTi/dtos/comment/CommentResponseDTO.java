package com.vaPaTi.vaPaTi.dtos.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private Long publicationId;
    private String text;
    private LocalDateTime createdAt;

    private Long userId;
    private String firstName;
    private String lastName;
    private String userName;
}
