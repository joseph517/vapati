package com.vaPaTi.vaPaTi.dtos.user;

import com.vaPaTi.vaPaTi.entity.user.SocialPlatform;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.validate;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateUserDTO - Profile URL fields Bean Validation")
class UpdateUserDtoProfileValidationTest {

    static Stream<Arguments> urlFields() {
        return Stream.of(
                Arguments.of("coverImageUrl", "Cover image must be at most 255 characters",
                        (BiConsumer<UpdateUserDTO, String>) UpdateUserDTO::setCoverImageUrl),
                Arguments.of("introVideoUrl", "Intro video must be at most 255 characters",
                        (BiConsumer<UpdateUserDTO, String>) UpdateUserDTO::setIntroVideoUrl),
                Arguments.of("website", "Website must be at most 255 characters",
                        (BiConsumer<UpdateUserDTO, String>) UpdateUserDTO::setWebsite)
        );
    }

    @ParameterizedTest(name = "{0} with 255 characters")
    @MethodSource("urlFields")
    @DisplayName("Each URL field accepts 255 characters")
    void urlFieldAtLimit(String field, String message, BiConsumer<UpdateUserDTO, String> setter) {
        // Given
        UpdateUserDTO dto = new UpdateUserDTO();
        setter.accept(dto, "a".repeat(255));

        // When & Then
        assertValid(dto);
    }

    @ParameterizedTest(name = "{0} with 256 characters")
    @MethodSource("urlFields")
    @DisplayName("Each URL field rejects 256 characters with its message")
    void urlFieldTooLong(String field, String message, BiConsumer<UpdateUserDTO, String> setter) {
        // Given
        UpdateUserDTO dto = new UpdateUserDTO();
        setter.accept(dto, "a".repeat(256));

        // When
        Set<ConstraintViolation<UpdateUserDTO>> violations = validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<UpdateUserDTO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath()).hasToString(field);
        assertThat(violation.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("A social link URL of 255 characters has no violations")
    void socialLinkUrlAtLimit() {
        // Given
        UpdateUserDTO dto = UpdateUserDTO.builder()
                .socialLinks(List.of(SocialLinkDTO.builder().platform(SocialPlatform.OTHER).url("a".repeat(255)).build()))
                .build();

        // When & Then
        assertValid(dto);
    }

    @Test
    @DisplayName("A social link URL of 256 characters is rejected on socialLinks[i].url")
    void socialLinkUrlTooLong() {
        // Given
        UpdateUserDTO dto = UpdateUserDTO.builder()
                .socialLinks(List.of(SocialLinkDTO.builder().platform(SocialPlatform.OTHER).url("a".repeat(256)).build()))
                .build();

        // When
        Set<ConstraintViolation<UpdateUserDTO>> violations = validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<UpdateUserDTO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("socialLinks[0].url");
        assertThat(violation.getMessage()).isEqualTo("Social link URL must be at most 255 characters");
    }
}
