package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;
    private UserInfo testUserInfo;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testUserInfo = new UserInfo();
        testUserInfo.setEmail("test@example.com");
        testUserInfo.setPassword("$2a$10$hashedPassword");
        testUserInfo.setUserName("testuser");
        testUserInfo.setFirstName("Test");
        testUserInfo.setLastName("User");

        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("USER");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUserInfo(testUserInfo);
        testUser.setRole(testRole);
        testUser.setActive(true);
        testUser.setDeletedAt(null);
    }

    @Nested
    @DisplayName("loadUserByUsername() - Active User Tests")
    class LoadActiveUserTests {

        @Test
        @DisplayName("Should load active user successfully by email")
        void loadUserByUsername_WithActiveUser_ShouldReturnUserDetails() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails)
                    .isNotNull()
                    .satisfies(details -> {
                        assertThat(details.getUsername()).isEqualTo("test@example.com");
                        assertThat(details.getPassword()).isEqualTo("$2a$10$hashedPassword");
                        assertThat(details.isEnabled()).isTrue();
                        assertThat(details.getAuthorities()).hasSize(1);
                        assertThat(details.getAuthorities())
                                .extracting(GrantedAuthority::getAuthority)
                                .containsExactly("ROLE_USER");
                    });

            verify(userRepository).findAllWithDetails();
            verify(userRepository, never()).findByEmailIncludingDeleted(anyString());
        }

        @Test
        @DisplayName("Should perform case-insensitive email matching")
        void loadUserByUsername_WithDifferentCase_ShouldFindUser() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("TEST@EXAMPLE.COM");

            // Then
            assertThat(userDetails)
                    .isNotNull()
                    .extracting(UserDetails::getUsername)
                    .isEqualTo("test@example.com");

            verify(userRepository).findAllWithDetails();
        }

        @Test
        @DisplayName("Should set correct authorities from role")
        void loadUserByUsername_WithUserRole_ShouldHaveUserAuthority() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("Should set UserDetails fields correctly")
        void loadUserByUsername_WithActiveUser_ShouldSetAllFieldsCorrectly() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
            assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("Should query all user details in one call")
        void loadUserByUsername_ShouldUseFindAllWithDetails() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            userDetailsService.loadUserByUsername("test@example.com");

            // Then
            verify(userRepository, times(1)).findAllWithDetails();
        }
    }

    @Nested
    @DisplayName("loadUserByUsername() - Deleted User Restoration Tests")
    class LoadDeletedUserTests {

        @Test
        @DisplayName("Should restore deleted user automatically")
        void loadUserByUsername_WithDeletedUser_ShouldRestoreAndReturnUserDetails() {
            // Given
            testUser.setDeletedAt(LocalDateTime.now().minusDays(1));
            when(userRepository.findAllWithDetails()).thenReturn(List.of());
            when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails)
                    .isNotNull()
                    .extracting(UserDetails::getUsername)
                    .isEqualTo("test@example.com");
            assertThat(testUser.getDeletedAt()).isNull();

            verify(userRepository).findAllWithDetails();
            verify(userRepository).findByEmailIncludingDeleted("test@example.com");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw exception when restored user is inactive")
        void loadUserByUsername_WithInactiveDeletedUser_ShouldThrowException() {
            // Given
            testUser.setActive(false);
            testUser.setDeletedAt(LocalDateTime.now().minusDays(1));
            when(userRepository.findAllWithDetails()).thenReturn(List.of());
            when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("test@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User account is disabled");

            verify(userRepository).save(testUser);
            assertThat(testUser.getDeletedAt()).isNull(); // Should still restore
        }

        @Test
        @DisplayName("Should check active users first before deleted users")
        void loadUserByUsername_ShouldCheckActiveUsersBeforeDeletedUsers() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of());
            when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("test@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class);

            // Verify execution order
            verify(userRepository).findAllWithDetails();
            verify(userRepository).findByEmailIncludingDeleted("test@example.com");
        }

        @Test
        @DisplayName("Should only restore users with deletedAt not null")
        void loadUserByUsername_WithDeletedAtNotNull_ShouldRestore() {
            // Given
            testUser.setDeletedAt(LocalDateTime.now().minusDays(5));
            when(userRepository.findAllWithDetails()).thenReturn(List.of());
            when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            // When
            userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(testUser.getDeletedAt()).isNull();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should save restored user to database")
        void loadUserByUsername_WhenRestoringUser_ShouldSaveToDatabase() {
            // Given
            testUser.setDeletedAt(LocalDateTime.now().minusDays(1));
            when(userRepository.findAllWithDetails()).thenReturn(List.of());
            when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            // When
            userDetailsService.loadUserByUsername("test@example.com");

            // Then
            verify(userRepository).save(testUser);
            assertThat(testUser.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("Should return valid UserDetails for restored user")
        void loadUserByUsername_WithRestoredUser_ShouldReturnValidUserDetails() {
            // Given
            testUser.setDeletedAt(LocalDateTime.now().minusDays(1));
            when(userRepository.findAllWithDetails()).thenReturn(List.of());
            when(userRepository.findByEmailIncludingDeleted("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails)
                    .isNotNull()
                    .satisfies(details -> {
                        assertThat(details.getUsername()).isEqualTo("test@example.com");
                        assertThat(details.getPassword()).isEqualTo("$2a$10$hashedPassword");
                        assertThat(details.isEnabled()).isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("loadUserByUsername() - Error Cases")
    class LoadUserErrorTests {

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found")
        void loadUserByUsername_WithNonExistentEmail_ShouldThrowException() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of());
            when(userRepository.findByEmailIncludingDeleted("nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with email: nonexistent@example.com");

            verify(userRepository).findAllWithDetails();
            verify(userRepository).findByEmailIncludingDeleted("nonexistent@example.com");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when active user is inactive")
        void loadUserByUsername_WithInactiveUser_ShouldThrowException() {
            // Given
            testUser.setActive(false);
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("test@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User account is disabled");

            verify(userRepository).findAllWithDetails();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle null email gracefully")
        void loadUserByUsername_WithNullEmail_ShouldThrowUsernameNotFoundException() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of());
            when(userRepository.findByEmailIncludingDeleted(null)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with email: null");
        }

        @Test
        @DisplayName("Should handle empty email gracefully")
        void loadUserByUsername_WithEmptyEmail_ShouldThrowNotFoundException() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of());
            when(userRepository.findByEmailIncludingDeleted(""))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with email: ");
        }
    }

    @Nested
    @DisplayName("loadUserByUsername() - Account Status Tests")
    class AccountStatusTests {

        @Test
        @DisplayName("Should set disabled=false for active users")
        void loadUserByUsername_WithActiveUser_ShouldSetEnabledTrue() {
            // Given
            testUser.setActive(true);
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should check user active status before creating UserDetails")
        void loadUserByUsername_ShouldCheckActiveStatusFirst() {
            // Given
            testUser.setActive(false);
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("test@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User account is disabled");
        }

        @Test
        @DisplayName("Should handle admin role correctly")
        void loadUserByUsername_WithAdminRole_ShouldHaveAdminAuthority() {
            // Given
            testRole.setName("ADMIN");
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("getAuthorities() tests")
    class GetAuthoritiesTests {

        @Test
        @DisplayName("Should convert role name to GrantedAuthority")
        void getAuthorities_WithUserRole_ShouldReturnUserAuthority() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("Should prefix role with ROLE_")
        void getAuthorities_ShouldPrefixWithRole() {
            // Given
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            String authority = userDetails.getAuthorities().iterator().next().getAuthority();
            assertThat(authority).startsWith("ROLE_");
        }

        @Test
        @DisplayName("Should convert role to uppercase")
        void getAuthorities_ShouldConvertToUppercase() {
            // Given
            testRole.setName("user"); // lowercase
            when(userRepository.findAllWithDetails()).thenReturn(List.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }
    }
}
