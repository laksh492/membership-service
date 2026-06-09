package com.firstclub.membership.exception;

public class MembershipCancelledException extends RuntimeException {
    public MembershipCancelledException(Long membershipId) {
        super("Membership is cancelled: " + membershipId);
    }
}
