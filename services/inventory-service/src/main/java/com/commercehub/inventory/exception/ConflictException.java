package com.commercehub.inventory.exception;

public class ConflictException extends InventoryException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
