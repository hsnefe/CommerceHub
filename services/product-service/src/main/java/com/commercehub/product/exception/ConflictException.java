package com.commercehub.product.exception;

public class ConflictException extends ProductException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
