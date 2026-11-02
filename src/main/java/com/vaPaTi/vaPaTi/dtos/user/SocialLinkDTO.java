package com.vaPaTi.vaPaTi.dtos.user;

import com.vaPaTi.vaPaTi.entity.user.SocialPlatform;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Used in the request of PUT /api/users/update and in the profile responses
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLinkDTO {
    private SocialPlatform platform;

    @Size(max = 255, message = "Social link URL must be at most 255 characters")
    private String url;
}
