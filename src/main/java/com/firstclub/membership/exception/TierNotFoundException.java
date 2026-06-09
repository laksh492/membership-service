package com.firstclub.membership.exception;

public class TierNotFoundException extends RuntimeException {
    public TierNotFoundException(Long tierId) {
        super("Membership tier not found: " + tierId);
    }
}
