package com.vaPaTi.vaPaTi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FollowersListResponseDto {
    private Long userId;
    private int totalFollowers;
    private List<FollowerUserDto> followers;
}
