package com.vaPaTi.vaPaTi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends MessageException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
