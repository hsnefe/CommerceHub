package com.commercehub.auth.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends AuthException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
