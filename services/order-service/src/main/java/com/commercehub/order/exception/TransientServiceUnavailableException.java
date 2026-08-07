package com.commercehub.order.exception;

public class TransientServiceUnavailableException extends ServiceUnavailableException {

    public TransientServiceUnavailableException(String message) {
        super(message);
    }
}
