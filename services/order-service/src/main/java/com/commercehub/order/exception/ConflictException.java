package com.commercehub.order.exception;

public class ConflictException extends OrderException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
