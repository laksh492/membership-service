package com.firstclub.membership.exception;

public class AlreadyOnTierException extends RuntimeException {
    public AlreadyOnTierException(Long tierId) {
        super("User is already on tier: " + tierId);
    }
}
