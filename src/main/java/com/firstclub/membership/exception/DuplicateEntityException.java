package com.firstclub.membership.exception;

public class DuplicateEntityException extends RuntimeException {
    public DuplicateEntityException(String entityType, Long id) {
        super("Duplicate " + entityType + " with id: " + id);
    }
}
