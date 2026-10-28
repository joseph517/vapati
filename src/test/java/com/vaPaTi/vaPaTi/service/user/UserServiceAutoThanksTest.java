package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.dtos.user.AutoThanksResponseDTO;
import com.vaPaTi.vaPaTi.dtos.user.UpdateAutoThanksDTO;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.user.UserAccountValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Auto Thanks Tests")
class UserServiceAutoThanksTest {

    private static final Long USER_ID = 1L;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private UserAccountValidationService userAccountValidationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        userInfo = UserInfo.builder().id(10L).userName("owner_user").build();
        user = User.builder().id(USER_ID).build();
        user.setUserInfo(userInfo);
    }

    private void givenAuthenticatedUser() {
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(USER_ID);
        when(userValidationService.getUserById(USER_ID)).thenReturn(user);
    }

    @Nested
    @DisplayName("getAutoThanksMessage")
    class GetAutoThanksMessageTests {

        @Test
        @DisplayName("Should return null when the user has not set an auto thanks message")
        void whenNotSet_ShouldReturnNull() {
            // Given
            givenAuthenticatedUser();

            // When
            AutoThanksResponseDTO result = userService.getAutoThanksMessage();

            // Then
            assertThat(result.getAutoThanksMessage()).isNull();
        }

        @Test
        @DisplayName("Should return the authenticated user's auto thanks message")
        void whenSet_ShouldReturnMessage() {
            // Given
            userInfo.setAutoThanksMessage("¡Gracias por tu apoyo!");
            givenAuthenticatedUser();

            // When
            AutoThanksResponseDTO result = userService.getAutoThanksMessage();

            // Then
            assertThat(result.getAutoThanksMessage()).isEqualTo("¡Gracias por tu apoyo!");
        }
    }

    @Nested
    @DisplayName("updateAutoThanksMessage")
    class UpdateAutoThanksMessageTests {

        @Test
        @DisplayName("Should store the trimmed message, update the userInfo timestamp and return it")
        void withValidMessage_ShouldStoreTrimmed() {
            // Given
            UpdateAutoThanksDTO dto = new UpdateAutoThanksDTO();
            dto.setAutoThanksMessage("  ¡Gracias por tu apoyo!  ");
            when(userValidationService.validateAndNormalizeAutoThanksMessage("  ¡Gracias por tu apoyo!  "))
                    .thenReturn("¡Gracias por tu apoyo!");
            givenAuthenticatedUser();

            // When
            AutoThanksResponseDTO result = userService.updateAutoThanksMessage(dto);

            // Then
            assertThat(result.getAutoThanksMessage()).isEqualTo("¡Gracias por tu apoyo!");
            assertThat(userInfo.getAutoThanksMessage()).isEqualTo("¡Gracias por tu apoyo!");
            verify(userAccountValidationService).updateUserInfoTimestamp(userInfo);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw and not save when the message is blank, keeping the previous one")
        void withBlankMessage_ShouldThrowAndNotSave() {
            // Given
            userInfo.setAutoThanksMessage("¡Gracias por tu apoyo!");
            UpdateAutoThanksDTO dto = new UpdateAutoThanksDTO();
            dto.setAutoThanksMessage("   ");
            when(userValidationService.validateAndNormalizeAutoThanksMessage("   "))
                    .thenThrow(new MessageException("Auto thanks message cannot be blank"));

            // When & Then
            assertThatThrownBy(() -> userService.updateAutoThanksMessage(dto))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Auto thanks message cannot be blank");
            assertThat(userInfo.getAutoThanksMessage()).isEqualTo("¡Gracias por tu apoyo!");
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteAutoThanksMessage")
    class DeleteAutoThanksMessageTests {

        @Test
        @DisplayName("Should leave the auto thanks message null, update the timestamp and return null")
        void whenSet_ShouldLeaveNull() {
            // Given
            userInfo.setAutoThanksMessage("¡Gracias por tu apoyo!");
            givenAuthenticatedUser();

            // When
            AutoThanksResponseDTO result = userService.deleteAutoThanksMessage();

            // Then
            assertThat(result.getAutoThanksMessage()).isNull();
            assertThat(userInfo.getAutoThanksMessage()).isNull();
            verify(userAccountValidationService).updateUserInfoTimestamp(userInfo);
            verify(userRepository).save(user);
        }
    }
}
