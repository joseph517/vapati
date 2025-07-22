package com.vaPaTi.vaPaTi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@RestController
public class GlobalExceptionHandler {

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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Access denied");
        error.put(MESSAGE, "You don't have permission to access this resource");
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
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

    @ExceptionHandler(MessageException.class)
    public ResponseEntity<Map<String, String>> handleMessageException(MessageException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, "Business logic error");
        error.put(MESSAGE, ex.getMessage());
        error.put(TIMESTAMP, LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}