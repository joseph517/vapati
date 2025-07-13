package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.UserCategoryRequestDTO;
import com.vaPaTi.vaPaTi.service.UserCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-categories")
public class UserCategoryController {

    private final UserCategoryService userCategoryService;

    public UserCategoryController(UserCategoryService userCategoryService) {
        this.userCategoryService = userCategoryService;
    }
}
