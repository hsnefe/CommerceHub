package com.commercehub.order.exception;

public class NotFoundException extends OrderException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
