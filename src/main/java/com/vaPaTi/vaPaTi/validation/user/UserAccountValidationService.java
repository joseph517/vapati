package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.dtos.user.CreateUserDTO;
import com.vaPaTi.vaPaTi.dtos.user.CreateUserInfoDTO;
import com.vaPaTi.vaPaTi.dtos.user.UpdateUserDTO;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.user.UserInfoMapper;
import com.vaPaTi.vaPaTi.repository.user.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

// Account data and credentials: creation, update of personal fields, password and email change
@Service
@RequiredArgsConstructor
public class UserAccountValidationService {

    private static final String DTO_NULL_ERROR = "DTO must not be null";
    private static final String USER_NULL_ERROR = "User must not be null";
    private static final Set<Character> SPECIAL_CHARS = Set.of(
            '!', '@', '#', '$', '%', '^', '&', '*', '(', ')',
            ',', '.', '?', '"', ':', '{', '}', '|', '<', '>'
    );
    private final PasswordEncoder passwordEncoder;
    private final UserInfoRepository userInfoRepository;
    private final UserInfoMapper userInfoMapper;

    public User createAndSetupUser(CreateUserDTO dto) {
        if (dto == null) {
            throw new MessageException(DTO_NULL_ERROR);
        }

        return User.builder()
                .verified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public UserInfo createUserInfo(CreateUserInfoDTO dto) {

        if (dto == null) {
            throw new MessageException(DTO_NULL_ERROR);
        }

        // Validations
        validateEmail(dto.getEmail(), null);
        validateUserName(dto.getUserName(), null);
        validatePhone(dto.getPhone(), null);
        validatePassword(dto.getPassword());

        UserInfo userInfo = userInfoMapper.fromCreateUserInfoDTO(dto);

        if (userInfo == null) {
            throw new MessageException("Mapped UserInfo is null");
        }

        // Encrypt the password AFTER mapping
        userInfo.setPassword(passwordEncoder.encode(dto.getPassword()));
        userInfo.setCreatedAt(LocalDateTime.now());
        userInfo.setUpdatedAt(LocalDateTime.now());

        return userInfo;
    }

    private void validateEmail(@NotNull String email, Long currentUserId) {
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new MessageException("Invalid email format");
        }

        Long userIdToExclude = (currentUserId != null) ? currentUserId : -1L;

        // check if email already exists (excluding the current user)
        if (userInfoRepository.existsByEmailAndUserIdNot(email, userIdToExclude)) {
            throw new ConflictException("Email already exists");
        }
    }

    private void validateUserName(@NotNull String userName, Long currentUserId) {
        if (!userName.matches("^[a-zA-Z0-9._-]+$")) {
            throw new MessageException("Username can only contain letters, numbers, '.', '_', and '-'");
        }

        if (userName.length() < 3 || userName.length() > 50) {
            throw new MessageException("Username must be between 3 and 50 characters");
        }

        Long userIdToExclude = (currentUserId != null) ? currentUserId : -1L;

        // check if username already exists (excluding the current user)
        if (userInfoRepository.existsByUserNameAndUserIdNot(userName, userIdToExclude)) {
            throw new ConflictException("Username already exists");
        }
    }

    private void validatePhone(String phone, Long currentUserId) {
        // A missing phone is rejected by the DTO validation; querying with null would match users without phone
        if (!isValidString(phone)) {
            return;
        }

        Long userIdToExclude = (currentUserId != null) ? currentUserId : -1L;

        // check if phone already exists (excluding the current user)
        if (userInfoRepository.existsByPhoneAndUserIdNot(phone, userIdToExclude)) {
            throw new ConflictException("Phone already exists");
        }
    }

    private static void validatePassword(@NotNull String password) {
        if (password.length() < 8) {
            throw new MessageException("Password must be at least 8 characters long");
        }

        PasswordCharacteristics chars = analyzePasswordCharacters(password);

        if (!chars.hasSpecial()) {
            throw new MessageException("Password must contain at least one special character");
        }
        if (!chars.hasUppercase()) {
            throw new MessageException("Password must contain at least one uppercase letter");
        }
        if (!chars.hasLowercase()) {
            throw new MessageException("Password must contain at least one lowercase letter");
        }
        if (!chars.hasDigit()) {
            throw new MessageException("Password must contain at least one number");
        }
    }

    private static PasswordCharacteristics analyzePasswordCharacters(String password) {
        boolean hasSpecial = false;
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (!hasSpecial && SPECIAL_CHARS.contains(c)) {
                hasSpecial = true;
            }
            if (!hasUppercase && Character.isUpperCase(c)) {
                hasUppercase = true;
            }
            if (!hasLowercase && Character.isLowerCase(c)) {
                hasLowercase = true;
            }
            if (!hasDigit && Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        return new PasswordCharacteristics(hasSpecial, hasUppercase, hasLowercase, hasDigit);
    }

    private record PasswordCharacteristics(
            boolean hasSpecial,
            boolean hasUppercase,
            boolean hasLowercase,
            boolean hasDigit
    ) {}

    public void updateUserInfoTimestamp(UserInfo userInfo) {
        if (userInfo == null) {
            throw new MessageException("UserInfo must not be null");
        }

        userInfo.setUpdatedAt(LocalDateTime.now());
    }

    // Returns true when the request changes credentials: a non-blank password, or an email different from the current one
    // (trimmed and lowercased). Then the current password is required and must match
    public boolean validateCredentialChange(User user, UpdateUserDTO dto) {
        if (dto == null) {
            throw new MessageException(DTO_NULL_ERROR);
        }

        UserInfo userInfo = getUserInfoOrThrow(user);
        if (!changesPassword(dto) && !changesEmail(dto, userInfo)) {
            return false;
        }

        if (!isValidString(dto.getCurrentPassword())) {
            throw new MessageException("Current password is required to change email or password");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), userInfo.getPassword())) {
            throw new MessageException("Current password is incorrect");
        }

        return true;
    }

    private boolean changesPassword(UpdateUserDTO dto) {
        return isValidString(dto.getPassword());
    }

    private boolean changesEmail(UpdateUserDTO dto, UserInfo userInfo) {
        if (!isValidString(dto.getEmail())) {
            return false;
        }
        String currentEmail = userInfo.getEmail() == null ? null : userInfo.getEmail().trim().toLowerCase();
        return !dto.getEmail().trim().toLowerCase().equals(currentEmail);
    }

    public void updateUserInfo( User user, UpdateUserDTO dto) {

        if (dto == null) {
            throw new MessageException(DTO_NULL_ERROR);
        }

        if (user == null) {
            throw new MessageException(USER_NULL_ERROR);
        }

        UserInfo userInfo = getUserInfoOrThrow(user);
        updateNameFields(dto, userInfo);
        updateEmail(dto, userInfo, user.getId());
        updateUserName(dto, userInfo, user.getId());
        updatePassword(dto, userInfo);
        updateOptionalFields(dto, userInfo, user.getId());
        updateUserInfoTimestamp(userInfo);
    }

    private UserInfo getUserInfoOrThrow(User user) {
        if (user == null) {
            throw new MessageException(USER_NULL_ERROR);
        }

        return Optional.ofNullable(user.getUserInfo())
                .orElseThrow(() -> new ResourceNotFoundException("UserInfo not found for user with id: " + user.getId()));
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

    private void updateOptionalFields(@NotNull UpdateUserDTO dto, @NotNull UserInfo userInfo, Long userId) {
        if (isValidString(dto.getPhone())) {
            validatePhone(dto.getPhone().trim(), userId);
            userInfo.setPhone(dto.getPhone().trim());
        }
        updateIfNotNull(dto.getDescription(), userInfo::setDescription);
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
}
