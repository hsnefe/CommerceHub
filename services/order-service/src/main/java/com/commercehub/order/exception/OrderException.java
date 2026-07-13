package com.commercehub.order.exception;

public class OrderException extends RuntimeException {

    private final String errorCode;

    public OrderException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
