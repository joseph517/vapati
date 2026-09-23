package com.vaPaTi.vaPaTi.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaPaTi.vaPaTi.dtos.CreateDonationDTO;
import com.vaPaTi.vaPaTi.dtos.CreateReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler - Unit Tests")
class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    record DonationWrapper(CreateDonationDTO donation) {
    }

    private HttpMessageNotReadableException unreadable(String json, Class<?> targetType) {
        Throwable jacksonException = catchThrowable(() -> objectMapper.readValue(json, targetType));
        return new HttpMessageNotReadableException(
                "JSON parse error: " + jacksonException.getMessage(), jacksonException, mock(HttpInputMessage.class));
    }

    private void assertNoInternalDetails(Map<String, String> body, Exception ex) {
        assertThat(body.values())
                .noneMatch(value -> value.contains(ex.getMessage()))
                .noneMatch(value -> value.contains("com.vaPaTi"))
                .noneMatch(value -> value.contains("java."))
                .noneMatch(value -> value.contains("Cannot deserialize"))
                .noneMatch(value -> value.contains("JSON parse error"));
    }

    @Nested
    @DisplayName("handleAccessDeniedException()")
    class AccessDeniedTests {

        @Test
        @DisplayName("Returns 403 with the same message as SecurityConfig and without the original exception message")
        void shouldReturnForbidden() {
            AccessDeniedException ex = new AccessDeniedException("Access Denied by PreAuthorize");

            ResponseEntity<Map<String, String>> response = handler.handleAccessDeniedException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody())
                    .containsEntry("error", "Forbidden")
                    .containsEntry("message", "You don't have permission to access this resource")
                    .containsKey("timestamp");
            assertThat(response.getBody().values()).noneMatch(value -> value.contains("PreAuthorize"));
        }
    }

    @Nested
    @DisplayName("handleHttpMessageNotReadableException()")
    class HttpMessageNotReadableTests {

        @Test
        @DisplayName("Invalid enum value names the field and lists the allowed values in declaration order")
        void shouldDescribeInvalidEnum() {
            HttpMessageNotReadableException ex = unreadable("{\"reason\": \"FOO\"}", CreateReportDTO.class);

            ResponseEntity<Map<String, String>> response = handler.handleHttpMessageNotReadableException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Malformed request")
                    .containsEntry("message", "Invalid value 'FOO' for field 'reason'. Allowed: SPAM, "
                            + "INAPPROPRIATE_CONTENT, HARASSMENT, FRAUD, MISINFORMATION, IMPERSONATION, OTHER")
                    .containsKey("timestamp");
            assertNoInternalDetails(response.getBody(), ex);
        }

        @Test
        @DisplayName("Wrong type in the body names the field and the value")
        void shouldDescribeInvalidType() {
            HttpMessageNotReadableException ex = unreadable("{\"amount\": \"abc\"}", CreateDonationDTO.class);

            ResponseEntity<Map<String, String>> response = handler.handleHttpMessageNotReadableException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Malformed request")
                    .containsEntry("message", "Invalid value 'abc' for field 'amount'");
            assertNoInternalDetails(response.getBody(), ex);
        }

        @Test
        @DisplayName("Wrong type in a nested object names the field with its dotted path")
        void shouldDescribeNestedFieldPath() {
            HttpMessageNotReadableException ex = unreadable(
                    "{\"donation\": {\"amount\": \"abc\"}}", DonationWrapper.class);

            ResponseEntity<Map<String, String>> response = handler.handleHttpMessageNotReadableException(ex);

            assertThat(response.getBody())
                    .containsEntry("message", "Invalid value 'abc' for field 'donation.amount'");
        }

        @Test
        @DisplayName("Missing body responds with its fixed message")
        void shouldDescribeMissingBody() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "Required request body is missing: public org.springframework.http.ResponseEntity "
                            + "com.vaPaTi.vaPaTi.controller.FollowerController.followUser(...)",
                    mock(HttpInputMessage.class));

            ResponseEntity<Map<String, String>> response = handler.handleHttpMessageNotReadableException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Malformed request")
                    .containsEntry("message", "Request body is required");
            assertNoInternalDetails(response.getBody(), ex);
        }

        @Test
        @DisplayName("Broken JSON responds with its fixed message")
        void shouldDescribeBrokenJson() {
            HttpMessageNotReadableException ex = unreadable("{\"amount\": ", CreateDonationDTO.class);

            ResponseEntity<Map<String, String>> response = handler.handleHttpMessageNotReadableException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Malformed request")
                    .containsEntry("message", "Malformed request body");
            assertNoInternalDetails(response.getBody(), ex);
        }
    }

    @Nested
    @DisplayName("handleMethodArgumentTypeMismatchException()")
    class MethodArgumentTypeMismatchTests {

        @Test
        @DisplayName("Names the parameter and the value")
        void shouldDescribeParameter() {
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                    "abc", Long.class, "campaignId", mock(MethodParameter.class),
                    new NumberFormatException("For input string: \"abc\""));

            ResponseEntity<Map<String, String>> response = handler.handleMethodArgumentTypeMismatchException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Invalid parameter")
                    .containsEntry("message", "Invalid value 'abc' for parameter 'campaignId'")
                    .containsKey("timestamp");
            assertNoInternalDetails(response.getBody(), ex);
        }
    }

    @Nested
    @DisplayName("handleMissingServletRequestParameterException()")
    class MissingServletRequestParameterTests {

        @Test
        @DisplayName("Names the missing parameter")
        void shouldDescribeMissingParameter() {
            MissingServletRequestParameterException ex = new MissingServletRequestParameterException("status", "String");

            ResponseEntity<Map<String, String>> response = handler.handleMissingServletRequestParameterException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Invalid parameter")
                    .containsEntry("message", "Required parameter 'status' is missing");
            assertNoInternalDetails(response.getBody(), ex);
        }
    }

    @Nested
    @DisplayName("handleHttpRequestMethodNotSupportedException()")
    class HttpRequestMethodNotSupportedTests {

        @Test
        @DisplayName("Returns 405 with the Allow header")
        void shouldReturnMethodNotAllowedWithAllowHeader() {
            HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE", List.of("GET"));

            ResponseEntity<Map<String, String>> response = handler.handleHttpRequestMethodNotSupportedException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(response.getHeaders().getAllow()).containsExactly(HttpMethod.GET);
            assertThat(response.getBody())
                    .containsEntry("error", "Method not allowed")
                    .containsEntry("message", "Method DELETE is not supported for this endpoint");
            assertNoInternalDetails(response.getBody(), ex);
        }
    }

    @Nested
    @DisplayName("handleHttpMediaTypeNotSupportedException()")
    class HttpMediaTypeNotSupportedTests {

        @Test
        @DisplayName("Returns 415 naming the content type")
        void shouldReturnUnsupportedMediaType() {
            HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
                    MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<Map<String, String>> response = handler.handleHttpMediaTypeNotSupportedException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            assertThat(response.getBody())
                    .containsEntry("error", "Unsupported media type")
                    .containsEntry("message", "Content type 'text/plain' is not supported. Use application/json");
            assertNoInternalDetails(response.getBody(), ex);
        }
    }

    @Nested
    @DisplayName("handleDataIntegrityViolationException()")
    class DataIntegrityViolationTests {

        @Test
        @DisplayName("Returns 409 with a generic message and without the SQL error")
        void shouldReturnGenericConflict() {
            DataIntegrityViolationException ex = new DataIntegrityViolationException(
                    "could not execute statement [Violation of UNIQUE KEY constraint 'UQ_user_info_phone'. "
                            + "Cannot insert duplicate key in object 'dbo.user_info'.] [insert into user_info ...]");

            ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolationException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody())
                    .containsEntry("error", "Conflict")
                    .containsEntry("message", "The request conflicts with existing data")
                    .containsKey("timestamp");
            assertNoInternalDetails(response.getBody(), ex);
            assertThat(response.getBody().values())
                    .noneMatch(value -> value.contains("UQ_user_info_phone"))
                    .noneMatch(value -> value.contains("insert into"));
        }
    }

    @Nested
    @DisplayName("handleHandlerMethodValidationException()")
    class HandlerMethodValidationTests {

        @Test
        @DisplayName("Returns 400 with the nested body field paths in fields")
        void shouldMapNestedBodyErrorsToFields() {
            ParameterErrors parameterErrors = mock(ParameterErrors.class);
            when(parameterErrors.getFieldErrors()).thenReturn(List.of(
                    new FieldError("request", "userInfo.phone", "must not be blank")));

            HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
            when(ex.getBeanResults()).thenReturn(List.of(parameterErrors));
            when(ex.getValueResults()).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response = handler.handleHandlerMethodValidationException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Validation failed")
                    .containsEntry("message", "One or more fields are invalid")
                    .containsKey("timestamp");
            assertThat(response.getBody().get("fields"))
                    .isEqualTo(Map.of("userInfo.phone", "must not be blank"));
        }

        @Test
        @DisplayName("Returns 400 with the parameter name in fields for a standalone parameter")
        void shouldMapStandaloneParameterErrorsToFields() {
            MethodParameter methodParameter = mock(MethodParameter.class);
            when(methodParameter.getParameterName()).thenReturn("page");

            ParameterValidationResult result = mock(ParameterValidationResult.class);
            when(result.getMethodParameter()).thenReturn(methodParameter);
            when(result.getResolvableErrors()).thenReturn(List.of(
                    new DefaultMessageSourceResolvable(null, null, "must be greater than or equal to 0")));

            HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
            when(ex.getBeanResults()).thenReturn(List.of());
            when(ex.getValueResults()).thenReturn(List.of(result));

            ResponseEntity<Map<String, Object>> response = handler.handleHandlerMethodValidationException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("error", "Validation failed");
            assertThat(response.getBody().get("fields"))
                    .isEqualTo(Map.of("page", "must be greater than or equal to 0"));
        }
    }
}
