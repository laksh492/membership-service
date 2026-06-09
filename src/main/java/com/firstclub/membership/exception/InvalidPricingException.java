package com.firstclub.membership.exception;

public class InvalidPricingException extends RuntimeException {
    public InvalidPricingException(String message) {
        super(message);
    }
}
