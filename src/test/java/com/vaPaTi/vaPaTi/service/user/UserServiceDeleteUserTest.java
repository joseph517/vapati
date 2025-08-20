package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - deleteUser() Tests")
public class UserServiceDeleteUserTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private UserService userService;

    private User activeUser;
    private User deletedUser;
    private final Long userId = 123L;

    @BeforeEach
    void setUp() {
        // Setup active user
        activeUser = User.builder()
                .id(userId)
                .active(true)
                .verified(true)
                .deletedAt(null)
                .build();

        // Setup already deleted user
        deletedUser = User.builder()
                .id(userId)
                .active(false)
                .verified(true)
                .deletedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
