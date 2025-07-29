package com.vaPaTi.vaPaTi.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateUserDTO {

    private Boolean active;

    private List<Long> categoryIds = new ArrayList<>();

}
