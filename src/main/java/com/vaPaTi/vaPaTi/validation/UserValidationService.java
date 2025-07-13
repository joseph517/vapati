package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.dtos.CreateUserDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateUserDTO;
import com.vaPaTi.vaPaTi.dtos.UserInfoDTO;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserCategory;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import com.vaPaTi.vaPaTi.repository.UserInfoRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class UserValidationService {

    private static final Number MAX_CATEGORIES_PER_USER = 6;
    private static final String USER_NOT_FOUND = "User not found";
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserInfoRepository userInfoRepository;
    private final UserRepository userRepository;

    // Constructor
    public UserValidationService(
            CategoryRepository categoryRepository,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            UserInfoRepository userInfoRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
    }

    public void validateCategoryLimit(@NotNull List<Long> categoryIds) {
        if (categoryIds.size() > MAX_CATEGORIES_PER_USER.intValue()) {
            throw new IllegalArgumentException("User cannot have more than " + MAX_CATEGORIES_PER_USER + " categories");
        }
    }

    public @NotNull List<Category> processCategories(List<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);

        Set<Long> foundIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = categoryIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new MessageException("Categories not found: " + missingIds);
        }

        return categories;
    }

    public @NotNull User createAndSetupUser(@NotNull CreateUserDTO dto) {
        User user = new User();

        // Prevent null to setIsActive
        user.setIsActive(dto.getActive() == null || dto.getActive());

        // False to setVerified
        user.setVerified(false);

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return user;
    }

    /**
     * Create a list of {@link UserCategory} objects from a list of categories
     * and a user, and set the user's categories.
     *
     * @param user    the user to create the categories for
     * @param categories the categories to create the relations for
     */
    public void createUserCategoryRelations(User user, @NotNull List<Category> categories) {
        List<UserCategory> userCategories = new ArrayList<>();

        for (Category category : categories) {
            UserCategory userCategory = new UserCategory();
            userCategory.setUser(user);
            userCategory.setCategory(category);
            userCategories.add(userCategory);
        }

        user.setUserCategories(userCategories);
    }

    public @NotNull UserInfo createUserInfo(@NotNull UserInfoDTO dto) {
        // check email, username, password before creating UserInfo
        validateEmail(dto.getEmail(), null);
        validateUserName(dto.getUserName(), null);
        validatePassword(dto.getPassword());

        UserInfo userInfo = new UserInfo();

        userInfo.setFirstName(dto.getFirstName());
        userInfo.setLastName(dto.getLastName());
        userInfo.setEmail(dto.getEmail());
        userInfo.setUserName(dto.getUserName());
        // encrypt password
        userInfo.setPassword(passwordEncoder.encode(dto.getPassword()));
        userInfo.setPhone(dto.getPhone());
        userInfo.setDescription(dto.getDescription());
        userInfo.setProfilePicture(dto.getProfilePicture());
        userInfo.setCreatedAt(LocalDateTime.now());
        userInfo.setUpdatedAt(LocalDateTime.now());
        return userInfo;
    }

    private void validateEmail(@NotNull String email, Long currentUserId) {
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        Long userIdToExclude = (currentUserId != null) ? currentUserId : -1L;

        // check if email already exists (excluding the current user)
        if (userInfoRepository.existsByEmailAndUserIdNot(email, userIdToExclude)) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private void validateUserName(@NotNull String userName, Long currentUserId) {
        if (userName.length() < 3 || userName.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }

        Long userIdToExclude = (currentUserId != null) ? currentUserId : -1L;

        // check if username already exists (excluding the current user)
        if (userInfoRepository.existsByUserNameAndUserIdNot(userName, userIdToExclude)) {
            throw new IllegalArgumentException("Username already exists");
        }
    }

    // TODO: implement password validation
    private static void validatePassword(@NotNull String password) {
        if (password.length() < 5) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

//        // Agregar más validaciones de contraseña según sea necesario
//        if (!password.matches(".*[A-Z].*")) {
//            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
//        }
//
//        if (!password.matches(".*[a-z].*")) {
//            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
//        }
//
//        if (!password.matches(".*\\d.*")) {
//            throw new IllegalArgumentException("Password must contain at least one number");
//        }
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));
    }

    public void updateTimestamp(@NotNull User user) {
        user.setUpdatedAt(LocalDateTime.now());
    }

    public void updateUserInfoTimestamp(@NotNull UserInfo userInfo) {
        userInfo.setUpdatedAt(LocalDateTime.now());
    }

    public void updateUserInfo(@NotNull User user, @NotNull UpdateUserDTO dto) {
        UserInfo userInfo = getUserInfoOrThrow(user);
        updateNameFields(dto, userInfo);
        updateEmail(dto, userInfo, user.getId());
        updateUserName(dto, userInfo, user.getId());
        updatePassword(dto, userInfo);
        updateOptionalFields(dto, userInfo);
        updateUserInfoTimestamp(userInfo);
    }

    private UserInfo getUserInfoOrThrow(@NotNull User user) {
        return Optional.ofNullable(user.getUserInfo())
                .orElseThrow(() -> new MessageException("UserInfo not found for user with id: " + user.getId()));
    }

    private boolean isValidString(String value) {
        return value != null && !value.isBlank();
    }

    private void updateNameFields(@NotNull UpdateUserDTO dto, @NotNull UserInfo userInfo) {
        updateIfValid(dto.getFirstName(), userInfo::setFirstName);
        updateIfValid(dto.getLastName(), userInfo::setLastName);
    }

    private void updateEmail(@NotNull UpdateUserDTO dto, UserInfo userInfo, Long userId) {
        if (isValidString(dto.getEmail())) {
            validateEmail(dto.getEmail(), userId);
            userInfo.setEmail(dto.getEmail().trim().toLowerCase());
        }
    }

    private void updateUserName(@NotNull UpdateUserDTO dto, UserInfo userInfo, Long userId) {
        if (isValidString(dto.getUserName())) {
            validateUserName(dto.getUserName(), userId);
            userInfo.setUserName(dto.getUserName().trim());
        }
    }

    private void updatePassword(@NotNull UpdateUserDTO dto, UserInfo userInfo) {
        if (isValidString(dto.getPassword())) {
            validatePassword(dto.getPassword());
            userInfo.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
    }

    private void updateOptionalFields(@NotNull UpdateUserDTO dto, @NotNull UserInfo userInfo) {
        updateIfValid(dto.getPhone(), userInfo::setPhone);
        updateIfNotNull(dto.getDescription(), userInfo::setDescription);
        updateIfNotNull(dto.getProfilePicture(), userInfo::setProfilePicture);
    }

    private void updateIfValid(String value, Consumer<String> setter) {
        if (isValidString(value)) {
            setter.accept(value.trim());
        }
    }

    private <T> void updateIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            if (value instanceof String stringValue) {
                setter.accept((T) stringValue.trim());
            } else {
                setter.accept(value);
            }
        }
    }

    public void updateUserCategories(@NotNull User user, @NotNull List<Category> newCategories) {
        Set<Long> newCategoryIds = newCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        // Eliminar categorías que no están en la nueva lista
        user.getUserCategories().removeIf(uc -> {
            if (!newCategoryIds.contains(uc.getCategory().getId())) {
                uc.setUser(null); // Desasociar
                return true;
            }
            return false;
        });

        // Añadir nuevas categorías
        Set<Long> existingIds = user.getUserCategories().stream()
                .map(uc -> uc.getCategory().getId())
                .collect(Collectors.toSet());

        newCategories.stream()
                .filter(c -> !existingIds.contains(c.getId()))
                .forEach(c -> {
                    UserCategory uc = new UserCategory();
                    uc.setUser(user);
                    uc.setCategory(c);
                    user.getUserCategories().add(uc);
                });
    }

    public void updateUserInfoPartial(UserInfo userInfo, @NotNull UpdateUserDTO dto) {
        if (dto.getFirstName() != null) userInfo.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) userInfo.setLastName(dto.getLastName());
        if (dto.getEmail() != null) userInfo.setEmail(dto.getEmail());
        if (dto.getUserName() != null) userInfo.setUserName(dto.getUserName());
        if (dto.getPassword() != null) userInfo.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getPhone() != null) userInfo.setPhone(dto.getPhone());
        if (dto.getDescription() != null) userInfo.setDescription(dto.getDescription());
        if (dto.getProfilePicture() != null) userInfo.setProfilePicture(dto.getProfilePicture());
    }

    public User getUserWithFullDetails(Long id) {
        return userRepository.findByIdWithFullDetails(id)
                .orElseThrow(() -> new MessageException(USER_NOT_FOUND));
    }

}
