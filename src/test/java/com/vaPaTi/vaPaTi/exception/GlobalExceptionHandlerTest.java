package com.vaPaTi.vaPaTi.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler - Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
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
