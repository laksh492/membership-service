package com.firstclub.membership.exception;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException(Long userId) {
        super("User profile not found: " + userId);
    }
}
