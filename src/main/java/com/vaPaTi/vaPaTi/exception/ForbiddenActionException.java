package com.vaPaTi.vaPaTi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenActionException extends MessageException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
