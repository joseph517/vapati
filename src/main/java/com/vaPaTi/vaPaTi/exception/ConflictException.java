package com.vaPaTi.vaPaTi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends MessageException {
    public ConflictException(String message) {
        super(message);
    }
}
