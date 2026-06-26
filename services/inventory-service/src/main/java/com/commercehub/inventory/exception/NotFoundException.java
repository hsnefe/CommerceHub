package com.commercehub.inventory.exception;

public class NotFoundException extends InventoryException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
