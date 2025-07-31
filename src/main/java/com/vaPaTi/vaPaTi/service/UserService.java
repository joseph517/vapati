package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.*;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.UserMapper;
import com.vaPaTi.vaPaTi.repository.RoleRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;
    private final UserValidationService userValidationService;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public List<UserDTO> listUsers() {
        return userRepository.findAllWithDetails().stream()
                .map(userMapper::toUserDTO)
                .toList();
    }

    // pagination method
    public Page<UserDTO> listUsersWithPagination(Pageable pageable) {
        Page<User> users = userRepository.findAllWithDetails(pageable);
        return users.map(userMapper::toUserDTO);
    }

    @Transactional
    public UserDTO createUser(@NotNull UserUserInfoRequestDTO request) {
        CreateUserDTO userDTO = request.getUser();
        CreateUserInfoDTO userInfoDTO = request.getUserInfo();

        // Validate category limit
        userValidationService.validateCategoryLimit(userDTO.getCategoryIds());

        // get categories in a single query
        List<Category> categories =  userValidationService.processCategories(userDTO.getCategoryIds());

        // Create user and set up
        User user = userValidationService.createAndSetupUser(userDTO);

        // Create user category relations
        userValidationService.createUserCategoryRelations(user, categories);

        // Set user info
        UserInfo userInfo = userValidationService.createUserInfo(userInfoDTO);

        // Role
        Role userRole = roleRepository.findByName("USER").orElseThrow(() -> new MessageException("Role not found"));

        user.setRole(userRole);
        user.setUserInfo(userInfo);
        userInfo.setUser(user);

        // Save user and return UserDTO
        User savedUser = userRepository.save(user);
        return userMapper.toUserDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUser( @NotNull UpdateUserDTO dto) {
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        User user = userValidationService.getUserById(userId);
        userValidationService.updateTimestamp(user);

        // Update user fields
        if (dto.getActive() != null) {
            user.setActive(dto.getActive());
        }

        // Update user info with full validations (only fields sent)
        userValidationService.updateUserInfo(user, dto);

        // Update categories only if explicitly provided
        if (dto.getCategoryIds() != null) {
            // Validate category limit (minimum 1, maximum 6)
            userValidationService.validateCategoryLimit(dto.getCategoryIds());

            // Process categories (validate that they exist)
            List<Category> categories = userValidationService.processCategories(dto.getCategoryIds());
            userValidationService.updateUserCategories(user, categories);
        }

        User savedUser = userRepository.save(user);
        return userMapper.toUserDTO(savedUser);
    }

    @Transactional
    public void deleteUser() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new MessageException("User is already deleted");
        }

        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void restoreUser(String email) {
        User user = userRepository.findByEmailIncludingDeleted(email)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));

        if (user.getDeletedAt() == null) {
            return;
        }

        user.setDeletedAt(null);
        userRepository.save(user);
    }

    public UserDTO getUserByIdDTO(Long id) {
        User user = userRepository.findByIdWithFullDetails(id)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));
        return userMapper.toUserDTO(user);
    }

}