package com.firstclub.membership.strategy;

import com.firstclub.membership.model.UserProfile;

public interface Criterion {
    boolean matches(UserProfile profile);
}
