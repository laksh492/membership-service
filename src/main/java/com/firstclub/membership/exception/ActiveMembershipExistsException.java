package com.firstclub.membership.exception;

public class ActiveMembershipExistsException extends RuntimeException {
    public ActiveMembershipExistsException(Long userId) {
        super("User already has an active membership: " + userId);
    }
}
