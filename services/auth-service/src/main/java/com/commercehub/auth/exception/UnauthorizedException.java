package com.commercehub.auth.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AuthException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
