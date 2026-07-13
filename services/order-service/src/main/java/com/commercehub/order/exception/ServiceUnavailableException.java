package com.commercehub.order.exception;

public class ServiceUnavailableException extends OrderException {

    public ServiceUnavailableException(String message) {
        super("SERVICE_UNAVAILABLE", message);
    }
}
