package com.vaPaTi.vaPaTi.validation.common;

import com.vaPaTi.vaPaTi.entity.user.Role;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleValidationService - Unit Tests")
class RoleValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleValidationService roleValidationService;

    @Nested
    @DisplayName("isAdmin")
    class IsAdminTests {

        @Test
        @DisplayName("Returns false for a null id without querying the database")
        void isAdmin_WithNullId_ShouldReturnFalse() {
            assertThat(roleValidationService.isAdmin(null)).isFalse();

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Returns true when the repository finds the user with the ADMIN role")
        void isAdmin_WhenUserIsAdmin_ShouldReturnTrue() {
            when(userRepository.existsByIdAndRole_Name(1L, "ADMIN")).thenReturn(true);

            assertThat(roleValidationService.isAdmin(1L)).isTrue();

            verify(userRepository).existsByIdAndRole_Name(1L, "ADMIN");
        }

        @Test
        @DisplayName("Returns false when the repository does not find the user with the ADMIN role")
        void isAdmin_WhenUserIsNotAdmin_ShouldReturnFalse() {
            when(userRepository.existsByIdAndRole_Name(2L, "ADMIN")).thenReturn(false);

            assertThat(roleValidationService.isAdmin(2L)).isFalse();

            verify(userRepository).existsByIdAndRole_Name(2L, "ADMIN");
        }
    }

    @Nested
    @DisplayName("hasAdminRole")
    class HasAdminRoleTests {

        @Test
        @DisplayName("Returns true for a user with the ADMIN role")
        void hasAdminRole_WithAdminUser_ShouldReturnTrue() {
            User user = User.builder().id(1L).role(Role.builder().id(2L).name("ADMIN").build()).build();

            assertThat(roleValidationService.hasAdminRole(user)).isTrue();
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Returns false for a user with the USER role")
        void hasAdminRole_WithRegularUser_ShouldReturnFalse() {
            User user = User.builder().id(1L).role(Role.builder().id(1L).name("USER").build()).build();

            assertThat(roleValidationService.hasAdminRole(user)).isFalse();
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Returns false for a user without a role")
        void hasAdminRole_WithNullRole_ShouldReturnFalse() {
            User user = User.builder().id(1L).role(null).build();

            assertThat(roleValidationService.hasAdminRole(user)).isFalse();
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Returns false for a null user")
        void hasAdminRole_WithNullUser_ShouldReturnFalse() {
            assertThat(roleValidationService.hasAdminRole(null)).isFalse();
            verifyNoInteractions(userRepository);
        }
    }
}
