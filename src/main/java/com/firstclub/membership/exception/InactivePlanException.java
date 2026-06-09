package com.firstclub.membership.exception;

public class InactivePlanException extends RuntimeException {
    public InactivePlanException(Long planId) {
        super("Membership plan is inactive: " + planId);
    }
}
