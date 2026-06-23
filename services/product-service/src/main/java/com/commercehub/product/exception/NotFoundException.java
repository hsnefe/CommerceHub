package com.commercehub.product.exception;

public class NotFoundException extends ProductException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
