package com.commercehub.inventory.exception;

public class ServiceUnavailableException extends InventoryException {

    public ServiceUnavailableException(String message) {
        super("SERVICE_UNAVAILABLE", message);
    }
}
