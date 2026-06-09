package com.firstclub.membership.exception;

public class MembershipExpiredException extends RuntimeException {
    public MembershipExpiredException(Long membershipId) {
        super("Membership has expired: " + membershipId);
    }
}
