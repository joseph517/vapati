package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.*;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.UserMapper;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.validation.UserValidationService;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

   // Error messages
    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;
    private final UserValidationService userValidationService;
    private final UserMapper userMapper;

    public UserService(
            UserRepository userRepository,
            UserValidationService userValidationService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.userValidationService = userValidationService;
        this.userMapper = userMapper;
    }

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
        UserInfoDTO userInfoDTO = request.getUserInfo();

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

        user.setUserInfo(userInfo);
        userInfo.setUser(user);

        // Save user and return UserDTO
        User savedUser = userRepository.save(user);
        return userMapper.toUserDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUser(Long id, @NotNull UpdateUserDTO dto) {
        User user = userValidationService.getUserById(id);
        userValidationService.updateTimestamp(user);

        // Update user fields
        if (dto.getActive() != null) {
            user.setIsActive(dto.getActive());
        }

        // Update user info (only non-null fields)
        userValidationService.updateUserInfoPartial(user.getUserInfo(), dto);

        // Update categories (including support for empty list)
        if (dto.getCategoryIds() != null) {
            List<Category> categories = userValidationService.processCategories(dto.getCategoryIds());
            userValidationService.updateUserCategories(user, categories);
        }

        User savedUser = userRepository.save(user);
        return userMapper.toUserDTO(savedUser);
    }

    @Transactional
    public ResponseEntity<Map<String, String>> deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new MessageException("User is already deleted");
        }

        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "User deleted successfully",
                "success", "true"
        ));
    }

    // Restore user TODO: implement
    @Transactional
    public ResponseEntity<Map<String, String>> restoreUser(Long id) {
        User user = userRepository.findDeletedById(id)
                .orElseThrow(() -> new MessageException("Deleted user not found"));

        user.setDeletedAt(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "User restored successfully",
                "success", "true"
        ));
    }

    public UserDTO getUserByIdAsDTO(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));
        return userMapper.toUserDTO(user);
    }

    public UserDTO getUserWithFullDetailsAsDTO(Long id) {
        User user = userRepository.findByIdWithFullDetails(id)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));
        return userMapper.toUserDTO(user);
    }

}