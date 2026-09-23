package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// Profile of another user: no email, phone, verified flag or bank accounts
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserProfileDTO {
    private Long id;
    @Builder.Default
    private List<String> categories = new ArrayList<>();
    private String firstName;
    private String lastName;
    private String userName;
    private String description;
    private String profilePicture;
}
