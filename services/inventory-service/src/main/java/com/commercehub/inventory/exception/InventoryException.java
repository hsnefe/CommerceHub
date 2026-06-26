package com.commercehub.inventory.exception;

public class InventoryException extends RuntimeException {

    private final String errorCode;

    public InventoryException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
