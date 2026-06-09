package com.firstclub.membership.exception;

public class InvalidTierChangeException extends RuntimeException {
    public InvalidTierChangeException(String message) {
        super(message);
    }
}
