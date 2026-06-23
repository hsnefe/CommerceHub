package com.commercehub.product.exception;

public class ProductException extends RuntimeException {

    private final String errorCode;

    public ProductException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
