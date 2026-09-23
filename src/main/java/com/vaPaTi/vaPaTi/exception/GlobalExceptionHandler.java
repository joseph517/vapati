package com.vaPaTi.vaPaTi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@RestController
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERROR_MESSAGE = "error";
    private static final String MESSAGE = "message";
    private static final String TIMESTAMP = "timestamp";

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Authentication failed");
        error.put(MESSAGE, ex.getMessage());
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "User not found");
        error.put(MESSAGE, ex.getMessage());
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Invalid credentials");
        error.put(MESSAGE, "Email or password is incorrect");
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Resource not found");
        error.put(MESSAGE, ex.getMessage());
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflictException(ConflictException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Conflict");
        error.put(MESSAGE, ex.getMessage());
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenActionException(ForbiddenActionException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Forbidden");
        error.put(MESSAGE, ex.getMessage());
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Forbidden");
        error.put(MESSAGE, "You don't have permission to access this resource");
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Invalid credentials");
        error.put(MESSAGE, ex.getMessage());
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MessageException.class)
    public ResponseEntity<Map<String, String>> handleMessageException(MessageException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Business logic error");
        error.put(MESSAGE, ex.getMessage());
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Validation failed");
        error.put(MESSAGE, "One or more fields are invalid");
        error.put("fields", fields);
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        Map<String, String> fields = new HashMap<>();
        for (ParameterErrors parameterErrors : ex.getBeanResults()) {
            for (FieldError fieldError : parameterErrors.getFieldErrors()) {
                fields.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        }
        for (ParameterValidationResult result : ex.getValueResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable resolvableError : result.getResolvableErrors()) {
                fields.put(parameterName, resolvableError.getDefaultMessage());
            }
        }

        Map<String, Object> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Validation failed");
        error.put(MESSAGE, "One or more fields are invalid");
        error.put("fields", fields);
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFoundException(NoResourceFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Resource not found");
        error.put(MESSAGE, "No endpoint found for this request");
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("Unhandled exception", ex);

        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Internal server error");
        error.put(MESSAGE, "An unexpected error occurred. Please try again later.");
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}