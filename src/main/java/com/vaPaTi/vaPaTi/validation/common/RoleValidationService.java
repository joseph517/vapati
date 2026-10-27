package com.vaPaTi.vaPaTi.validation.common;

import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// The ADMIN role check shared by every domain. Read from the database, not from the token claim, which goes stale if the role changes
@Service
@RequiredArgsConstructor
public class RoleValidationService {

    public static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;

    /**
     * Whether the user is an ADMIN, read from the database in one SELECT. False for a null id (anonymous)
     * and for a deleted user (@SQLRestriction hides it).
     */
    public boolean isAdmin(Long userId) {
        return userId != null && userRepository.existsByIdAndRole_Name(userId, ADMIN_ROLE);
    }

    /**
     * Same rule on a user already loaded with its role, so no extra query is made.
     */
    public boolean hasAdminRole(User user) {
        return user != null && user.getRole() != null && ADMIN_ROLE.equals(user.getRole().getName());
    }
}
