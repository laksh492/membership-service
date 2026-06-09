package com.firstclub.membership.exception;

public class TierCriteriaNotMetException extends RuntimeException {
    public TierCriteriaNotMetException(Long tierId) {
        super("Tier upgrade criteria not met for tier: " + tierId);
    }
}
