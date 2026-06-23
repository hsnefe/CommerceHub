package com.commercehub.auth.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AuthException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
