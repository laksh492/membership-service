package com.firstclub.membership.exception;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(Long planId) {
        super("Membership plan not found: " + planId);
    }
}
