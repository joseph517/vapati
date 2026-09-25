package com.vaPaTi.vaPaTi.dtos.follower;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FollowersListResponseDTO {
    private Long userId;
    private int totalFollowers;
    private List<FollowerUserDTO> followers;
}
