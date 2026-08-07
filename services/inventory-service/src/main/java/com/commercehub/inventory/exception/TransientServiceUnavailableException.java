package com.commercehub.inventory.exception;

public class TransientServiceUnavailableException extends ServiceUnavailableException {

    public TransientServiceUnavailableException(String message) {
        super(message);
    }
}
