package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.UserCategoryRequestDTO;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserCategory;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.repository.UserCategoryRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;


@Service
public class UserCategoryService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UserCategoryRepository userCategoryRepository;

    public UserCategoryService(UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               UserCategoryRepository userCategoryRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.userCategoryRepository = userCategoryRepository;
    }

//    public String assignCategoryToUser(@NotNull UserCategoryRequestDTO request) {
//        User user = userRepository.findById(request.getUserId())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//        Category category = categoryRepository.findById(request.getCategoryId())
//                .orElseThrow(() -> new RuntimeException("Category not found"));
//
//        // check if user already has this category
//        if (userCategoryRepository.existsByUserAndCategory(user, category)) {
//            return "User already has this category";
//        }
//
//        // valid category limit
//        long count = userCategoryRepository.countByUser(user);
//        if (count >= 6) {
//            return "User already has maximum of 6 categories";
//        }
//
//        // add category
//        UserCategory userCategory = new UserCategory();
//        userCategory.setUser(user);
//        userCategory.setCategory(category);
//        userCategoryRepository.save(userCategory);
//
//        return "Category assigned successfully";
//    }

}
