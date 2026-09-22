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
            when(userRepository.findByEmailIncludingDeleted("test@example.com")).thenReturn(Optional.of(testUser));

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

            verify(userRepository).findByEmailIncludingDeleted("test@example.com");
            verify(userRepository, never()).findAllWithDetails();
        }

        @Test
        @DisplayName("Should pass the email as received (the DB collation is case insensitive)")
        void loadUserByUsername_WithDifferentCase_ShouldFindUser() {
            // Given
            when(userRepository.findByEmailIncludingDeleted("TEST@EXAMPLE.COM")).thenReturn(Optional.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("TEST@EXAMPLE.COM");

            // Then
            assertThat(userDetails)
                    .isNotNull()
                    .extracting(UserDetails::getUsername)
                    .isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should set UserDetails fields correctly")
        void loadUserByUsername_WithActiveUser_ShouldSetAllFieldsCorrectly() {
            // Given
            when(userRepository.findByEmailIncludingDeleted("test@example.com")).thenReturn(Optional.of(testUser));

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
    }

    @Nested
    @DisplayName("loadUserByUsername() - Deleted User Tests (no restoration)")
    class LoadDeletedUserTests {

        @Test
        @DisplayName("Should load a deleted user without restoring it")
        void loadUserByUsername_WithDeletedUser_ShouldNotRestore() {
            // Given
            LocalDateTime deletedAt = LocalDateTime.now().minusDays(1);
            testUser.setDeletedAt(deletedAt);
            when(userRepository.findByEmailIncludingDeleted("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then - the password can be validated, but restoration is up to AuthenticationService
            assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
            assertThat(testUser.getDeletedAt()).isEqualTo(deletedAt);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw for a deleted and inactive user without restoring it")
        void loadUserByUsername_WithInactiveDeletedUser_ShouldThrowWithoutRestoring() {
            // Given
            LocalDateTime deletedAt = LocalDateTime.now().minusDays(1);
            testUser.setActive(false);
            testUser.setDeletedAt(deletedAt);
            when(userRepository.findByEmailIncludingDeleted("test@example.com")).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("test@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User account is disabled");

            assertThat(testUser.getDeletedAt()).isEqualTo(deletedAt);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Never calls save with any user")
        void loadUserByUsername_NeverSaves() {
            // Given
            testUser.setDeletedAt(LocalDateTime.now().minusDays(5));
            when(userRepository.findByEmailIncludingDeleted("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            userDetailsService.loadUserByUsername("test@example.com");

            // Then
            verify(userRepository, never()).save(any());
            verify(userRepository, never()).saveAndFlush(any());
            verify(userRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("loadUserByUsername() - Error Cases")
    class LoadUserErrorTests {

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found")
        void loadUserByUsername_WithNonExistentEmail_ShouldThrowException() {
            // Given
            when(userRepository.findByEmailIncludingDeleted("nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with email: nonexistent@example.com");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user is inactive")
        void loadUserByUsername_WithInactiveUser_ShouldThrowException() {
            // Given
            testUser.setActive(false);
            when(userRepository.findByEmailIncludingDeleted("test@example.com")).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("test@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User account is disabled");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle null email gracefully")
        void loadUserByUsername_WithNullEmail_ShouldThrowUsernameNotFoundException() {
            // Given
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
            when(userRepository.findByEmailIncludingDeleted("")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with email: ");
        }
    }

    @Nested
    @DisplayName("getAuthorities() tests")
    class GetAuthoritiesTests {

        @Test
        @DisplayName("Should convert role name to GrantedAuthority with ROLE_ prefix")
        void getAuthorities_WithUserRole_ShouldReturnUserAuthority() {
            // Given
            when(userRepository.findByEmailIncludingDeleted("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("Should handle admin role correctly")
        void loadUserByUsername_WithAdminRole_ShouldHaveAdminAuthority() {
            // Given
            testRole.setName("ADMIN");
            when(userRepository.findByEmailIncludingDeleted("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Should convert role to uppercase")
        void getAuthorities_ShouldConvertToUppercase() {
            // Given
            testRole.setName("user"); // lowercase
            when(userRepository.findByEmailIncludingDeleted("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

            // Then
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }
    }
}
