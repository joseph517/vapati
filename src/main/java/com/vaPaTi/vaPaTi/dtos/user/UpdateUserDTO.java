package com.vaPaTi.vaPaTi.dtos.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserDTO {
    @Nullable
    private List<Long> categoryIds;

    @Nullable
    @Size(max = 255)
    private String firstName;
    @Nullable
    @Size(max = 255)
    private String lastName;
    @Nullable
    @Size(max = 254)
    private String email;
    @Nullable
    private String userName;
    @Nullable
    private String password;
    @Nullable
    @Size(max = 20)
    private String phone;
    @Nullable
    @Size(max = 255)
    private String description;
    @Nullable
    @Size(max = 255)
    private String profilePicture;
    @Nullable
    @Size(max = 255, message = "Cover image must be at most 255 characters")
    private String coverImageUrl;
    @Nullable
    @Size(max = 255, message = "Intro video must be at most 255 characters")
    private String introVideoUrl;
    @Nullable
    @Size(max = 255, message = "Website must be at most 255 characters")
    private String website;

    // null keeps the current links; a list replaces them all ([] removes them)
    @Nullable
    private List<@Valid SocialLinkDTO> socialLinks;

    // Required only when the request changes the password or the email
    @Nullable
    private String currentPassword;
}
