package com.vaPaTi.vaPaTi.dtos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertSingleViolation;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;

@DisplayName("User DTOs - Bean Validation")
class UserDtoValidationTest {

    @Nested
    @DisplayName("UserUserInfoRequestDTO / CreateUserInfoDTO")
    class CreateUserTests {

        private UserUserInfoRequestDTO request;

        @BeforeEach
        void setUp() {
            CreateUserDTO user = CreateUserDTO.builder().categoryIds(List.of(1L)).build();
            CreateUserInfoDTO userInfo = CreateUserInfoDTO.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .userName("johndoe")
                    .password("Password123!")
                    .phone("+1234567890")
                    .description("")
                    .profilePicture("profile.jpg")
                    .build();

            request = new UserUserInfoRequestDTO();
            request.setUser(user);
            request.setUserInfo(userInfo);
        }

        static Stream<Arguments> requiredFields() {
            return Stream.of(
                    Arguments.of("firstName", (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setFirstName),
                    Arguments.of("lastName", (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setLastName),
                    Arguments.of("email", (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setEmail),
                    Arguments.of("userName", (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setUserName),
                    Arguments.of("password", (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setPassword),
                    Arguments.of("phone", (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setPhone),
                    Arguments.of("description", (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setDescription)
            );
        }

        static Stream<Arguments> notBlankFields() {
            return requiredFields().filter(arguments -> !"description".equals(arguments.get()[0]));
        }

        static Stream<Arguments> sizedFields() {
            return Stream.of(
                    Arguments.of("firstName", 255, (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setFirstName),
                    Arguments.of("lastName", 255, (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setLastName),
                    Arguments.of("email", 254, (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setEmail),
                    Arguments.of("phone", 20, (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setPhone),
                    Arguments.of("description", 255, (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setDescription),
                    Arguments.of("profilePicture", 255, (BiConsumer<CreateUserInfoDTO, String>) CreateUserInfoDTO::setProfilePicture)
            );
        }

        @Test
        @DisplayName("A valid request has no violations")
        void validRequestHasNoViolations() {
            assertValid(request);
        }

        @Test
        @DisplayName("Missing user is a violation on 'user'")
        void missingUser() {
            request.setUser(null);

            assertSingleViolation(request, "user");
        }

        @Test
        @DisplayName("Missing userInfo is a violation on 'userInfo'")
        void missingUserInfo() {
            request.setUserInfo(null);

            assertSingleViolation(request, "userInfo");
        }

        @ParameterizedTest(name = "missing {0}")
        @MethodSource("requiredFields")
        @DisplayName("Each missing required field is a violation with its nested path")
        void missingRequiredField(String field, BiConsumer<CreateUserInfoDTO, String> setter) {
            setter.accept(request.getUserInfo(), null);

            assertSingleViolation(request, "userInfo." + field);
        }

        @ParameterizedTest(name = "blank {0}")
        @MethodSource("notBlankFields")
        @DisplayName("Each @NotBlank field rejects a blank value")
        void blankField(String field, BiConsumer<CreateUserInfoDTO, String> setter) {
            setter.accept(request.getUserInfo(), "   ");

            assertSingleViolation(request, "userInfo." + field);
        }

        @Test
        @DisplayName("An empty description is accepted")
        void emptyDescriptionIsAccepted() {
            request.getUserInfo().setDescription("");

            assertValid(request);
        }

        @Test
        @DisplayName("profilePicture is optional")
        void profilePictureIsOptional() {
            request.getUserInfo().setProfilePicture(null);

            assertValid(request);
        }

        @ParameterizedTest(name = "{0} over {1} characters")
        @MethodSource("sizedFields")
        @DisplayName("Each sized field rejects one character over its limit")
        void fieldTooLong(String field, int max, BiConsumer<CreateUserInfoDTO, String> setter) {
            setter.accept(request.getUserInfo(), "a".repeat(max + 1));

            assertSingleViolation(request, "userInfo." + field);
        }

        @ParameterizedTest(name = "{0} with {1} characters")
        @MethodSource("sizedFields")
        @DisplayName("Each sized field accepts exactly its limit")
        void fieldAtLimit(String field, int max, BiConsumer<CreateUserInfoDTO, String> setter) {
            setter.accept(request.getUserInfo(), "a".repeat(max));

            assertValid(request);
        }
    }

    @Nested
    @DisplayName("UpdateUserDTO")
    class UpdateUserTests {

        static Stream<Arguments> sizedFields() {
            return Stream.of(
                    Arguments.of("firstName", 255, (BiConsumer<UpdateUserDTO, String>) UpdateUserDTO::setFirstName),
                    Arguments.of("lastName", 255, (BiConsumer<UpdateUserDTO, String>) UpdateUserDTO::setLastName),
                    Arguments.of("email", 254, (BiConsumer<UpdateUserDTO, String>) UpdateUserDTO::setEmail),
                    Arguments.of("phone", 20, (BiConsumer<UpdateUserDTO, String>) UpdateUserDTO::setPhone),
                    Arguments.of("description", 255, (BiConsumer<UpdateUserDTO, String>) UpdateUserDTO::setDescription),
                    Arguments.of("profilePicture", 255, (BiConsumer<UpdateUserDTO, String>) UpdateUserDTO::setProfilePicture)
            );
        }

        @Test
        @DisplayName("An empty DTO has no violations")
        void emptyDtoHasNoViolations() {
            assertValid(new UpdateUserDTO());
        }

        @ParameterizedTest(name = "{0} over {1} characters")
        @MethodSource("sizedFields")
        @DisplayName("Each sized field rejects one character over its limit")
        void fieldTooLong(String field, int max, BiConsumer<UpdateUserDTO, String> setter) {
            UpdateUserDTO dto = new UpdateUserDTO();
            setter.accept(dto, "a".repeat(max + 1));

            assertSingleViolation(dto, field);
        }

        @ParameterizedTest(name = "{0} with {1} characters")
        @MethodSource("sizedFields")
        @DisplayName("Each sized field accepts exactly its limit")
        void fieldAtLimit(String field, int max, BiConsumer<UpdateUserDTO, String> setter) {
            UpdateUserDTO dto = new UpdateUserDTO();
            setter.accept(dto, "a".repeat(max));

            assertValid(dto);
        }
    }
}
