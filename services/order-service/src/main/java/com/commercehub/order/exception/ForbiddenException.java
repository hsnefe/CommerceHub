package com.commercehub.order.exception;

public class ForbiddenException extends OrderException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
