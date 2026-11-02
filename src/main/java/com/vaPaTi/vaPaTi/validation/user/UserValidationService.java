package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserValidationService {

    private static final String USER_NOT_FOUND = "User not found";
    private static final String USER_NULL_ERROR = "User must not be null";
    private final UserRepository userRepository;
    private final AccountStatusValidationService accountStatusValidationService;

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
    }

    // A banned or currently suspended user is hidden like a missing one: 404 without revealing the sanction
    public void validateProfileNotSanctioned(User user) {
        if (accountStatusValidationService.isSanctioned(user)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND);
        }
    }

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public void updateTimestamp(User user) {
        if (user == null) {
            throw new MessageException(USER_NULL_ERROR);
        }

        user.setUpdatedAt(LocalDateTime.now());
    }
}
