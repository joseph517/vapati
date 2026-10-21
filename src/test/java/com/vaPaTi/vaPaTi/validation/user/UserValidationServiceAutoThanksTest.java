package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.user.UserInfoMapper;
import com.vaPaTi.vaPaTi.repository.category.CategoryRepository;
import com.vaPaTi.vaPaTi.repository.user.UserInfoRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserValidationService - Auto Thanks Tests")
class UserValidationServiceAutoThanksTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserInfoMapper userInfoMapper;

    @Mock
    private AccountStatusValidationService accountStatusValidationService;

    // Real instance: the text rule lives in TextValidationService
    @Spy
    private TextValidationService textValidationService = new TextValidationService();

    @InjectMocks
    private UserValidationService userValidationService;

    @Test
    @DisplayName("Should return the auto thanks message without leading and trailing spaces")
    void validateAndNormalizeAutoThanksMessage_WithSurroundingSpaces_ShouldReturnTrimmed() {
        // When
        String result = userValidationService.validateAndNormalizeAutoThanksMessage("  ¡Gracias por tu apoyo!  ");

        // Then
        assertThat(result).isEqualTo("¡Gracias por tu apoyo!");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    @DisplayName("Should throw when the auto thanks message is missing, empty or only whitespace")
    void validateAndNormalizeAutoThanksMessage_WhenBlank_ShouldThrow(String autoThanksMessage) {
        // When & Then
        assertThatThrownBy(() -> userValidationService.validateAndNormalizeAutoThanksMessage(autoThanksMessage))
                .isInstanceOf(MessageException.class)
                .hasMessage("Auto thanks message cannot be blank");
    }

    @Test
    @DisplayName("Should accept an auto thanks message of exactly 280 characters")
    void validateAndNormalizeAutoThanksMessage_With280Characters_ShouldReturnMessage() {
        // Given
        String autoThanksMessage = "a".repeat(280);

        // When
        String result = userValidationService.validateAndNormalizeAutoThanksMessage(autoThanksMessage);

        // Then
        assertThat(result).isEqualTo(autoThanksMessage);
    }

    @Test
    @DisplayName("Should throw when the auto thanks message has more than 280 characters")
    void validateAndNormalizeAutoThanksMessage_With281Characters_ShouldThrow() {
        // When & Then
        assertThatThrownBy(() -> userValidationService.validateAndNormalizeAutoThanksMessage("a".repeat(281)))
                .isInstanceOf(MessageException.class)
                .hasMessage("Auto thanks message must be at most 280 characters");
    }
}
