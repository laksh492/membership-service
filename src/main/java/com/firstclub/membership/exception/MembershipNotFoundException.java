package com.firstclub.membership.exception;

public class MembershipNotFoundException extends RuntimeException {
    public MembershipNotFoundException(Long userId) {
        super("Membership not found for user: " + userId);
    }
}
