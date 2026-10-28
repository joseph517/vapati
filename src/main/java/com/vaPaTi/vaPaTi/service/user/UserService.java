package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.dtos.user.AutoThanksResponseDTO;
import com.vaPaTi.vaPaTi.dtos.user.CreateUserDTO;
import com.vaPaTi.vaPaTi.dtos.user.CreateUserInfoDTO;
import com.vaPaTi.vaPaTi.dtos.user.UpdateAutoThanksDTO;
import com.vaPaTi.vaPaTi.dtos.user.UpdateUserDTO;
import com.vaPaTi.vaPaTi.dtos.user.UserDTO;
import com.vaPaTi.vaPaTi.dtos.user.UserUserInfoRequestDTO;
import com.vaPaTi.vaPaTi.entity.category.Category;
import com.vaPaTi.vaPaTi.entity.user.Role;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.user.UserMapper;
import com.vaPaTi.vaPaTi.repository.user.RoleRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.campaign.CampaignService;
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserAccountValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserCategoryValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserProfileValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    private final CampaignService campaignService;
    private final UserCategoryValidationService userCategoryValidationService;
    private final UserAccountValidationService userAccountValidationService;
    private final UserProfileValidationService userProfileValidationService;
    private final RoleValidationService roleValidationService;

    public List<UserDTO> listUsers() {
        return userRepository.findAllWithDetails().stream()
                .map(userMapper::toUserDTO)
                .toList();
    }

    // Two steps: the ids of the page are paginated in SQL, then those users are loaded with their collections.
    // Fetching the collections in the paginated query made Hibernate paginate in memory (HHH90003004)
    public Page<UserDTO> listUsersWithPagination(Pageable pageable) {
        Page<Long> ids = userRepository.findPageOfIds(pageable);
        if (!ids.hasContent()) {
            return new PageImpl<>(List.of(), pageable, ids.getTotalElements());
        }

        Map<Long, User> usersById = new HashMap<>();
        for (User user : userRepository.findAllWithDetailsByIdIn(ids.getContent())) {
            usersById.put(user.getId(), user);
        }

        // Keeps the order of the page. A user deleted between both queries is skipped
        List<UserDTO> users = ids.getContent().stream()
                .map(usersById::get)
                .filter(Objects::nonNull)
                .map(userMapper::toUserDTO)
                .toList();
        return new PageImpl<>(users, pageable, ids.getTotalElements());
    }

    @Transactional
    public UserDTO createUser(@NotNull UserUserInfoRequestDTO request) {
        
        CreateUserDTO userDTO = request.getUser();
        CreateUserInfoDTO userInfoDTO = request.getUserInfo();

        // Validate category limit
        userCategoryValidationService.validateCategoryLimit(userDTO.getCategoryIds());

        // get categories in a single query
        List<Category> categories =  userCategoryValidationService.processCategories(userDTO.getCategoryIds());

        // Create user and set up
        User user = userAccountValidationService.createAndSetupUser(userDTO);

        // Create user category relations
        userCategoryValidationService.createUserCategoryRelations(user, categories);

        // Set user info
        UserInfo userInfo = userAccountValidationService.createUserInfo(userInfoDTO);

        // Role
        Role userRole = roleRepository.findByName("USER").orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        user.setRole(userRole);
        user.setUserInfo(userInfo);
        userInfo.setUser(user);

        // Save user and return UserDTO
        User savedUser = userRepository.save(user);
        return userMapper.toUserDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUser( UpdateUserDTO dto) {
        if (dto == null) {
            throw new MessageException("DTO cannot be null");
        }
        
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        User user = userValidationService.getUserById(userId);

        // Before any field is modified: changing the email or password requires the current password
        boolean credentialsChanged = userAccountValidationService.validateCredentialChange(user, dto);

        userValidationService.updateTimestamp(user);

        // Update user info with full validations (only fields sent)
        userAccountValidationService.updateUserInfo(user, dto);

        // Update categories only if explicitly provided
        if (dto.getCategoryIds() != null) {
            // Validate category limit (minimum 1, maximum 6)
            userCategoryValidationService.validateCategoryLimit(dto.getCategoryIds());

            // Process categories (validate that they exist)
            List<Category> categories = userCategoryValidationService.processCategories(dto.getCategoryIds());
            userCategoryValidationService.updateUserCategories(user, categories);
        }

        // Invalidates every access and refresh token issued so far, including the current session
        if (credentialsChanged) {
            user.setTokensValidAfter(LocalDateTime.now());
        }

        User savedUser = userRepository.save(user);
        return userMapper.toUserDTO(savedUser);
    }

    @Transactional
    public void deleteUser() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new ConflictException("User is already deleted");
        }

        campaignService.closeAllByOwner(userId);

        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // The user themselves and any ADMIN get the full UserDTO; anyone else gets the PublicUserProfileDTO,
    // or a 404 if the user is banned or suspended
    public Object getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new MessageException("ID must be a positive number");
        }
        
        User user = userRepository.findByIdWithFullDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        Long callerId = authenticatedUserService.getAuthenticatedUserId();
        if (id.equals(callerId) || roleValidationService.isAdmin(callerId)) {
            return userMapper.toUserDTO(user);
        }
        userValidationService.validateProfileNotSanctioned(user);
        return userMapper.toPublicUserProfileDTO(user);
    }

    public AutoThanksResponseDTO getAutoThanksMessage() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        User user = userValidationService.getUserById(userId);

        UserInfo userInfo = user.getUserInfo();
        return new AutoThanksResponseDTO(userInfo != null ? userInfo.getAutoThanksMessage() : null);
    }

    @Transactional
    public AutoThanksResponseDTO updateAutoThanksMessage(UpdateAutoThanksDTO dto) {
        if (dto == null) {
            throw new MessageException("DTO cannot be null");
        }

        // Before loading the user: a blank text throws without touching anything
        String autoThanksMessage = userProfileValidationService.validateAndNormalizeAutoThanksMessage(dto.getAutoThanksMessage());

        Long userId = authenticatedUserService.getAuthenticatedUserId();
        User user = userValidationService.getUserById(userId);

        UserInfo userInfo = user.getUserInfo();
        userAccountValidationService.updateUserInfoTimestamp(userInfo);
        userInfo.setAutoThanksMessage(autoThanksMessage);

        userRepository.save(user);
        return new AutoThanksResponseDTO(autoThanksMessage);
    }

    @Transactional
    public AutoThanksResponseDTO deleteAutoThanksMessage() {
        Long userId = authenticatedUserService.getAuthenticatedUserId();
        User user = userValidationService.getUserById(userId);

        UserInfo userInfo = user.getUserInfo();
        userAccountValidationService.updateUserInfoTimestamp(userInfo);
        userInfo.setAutoThanksMessage(null);

        userRepository.save(user);
        return new AutoThanksResponseDTO(null);
    }

}