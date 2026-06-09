package com.firstclub.membership.strategy;

import com.firstclub.membership.model.UserProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MinOrderCountCriterion implements Criterion {
    private final int minOrderCount;

    @Override
    public boolean matches(UserProfile profile) {
        return profile.getTotalOrderCount() >= minOrderCount;
    }

    @Override
    public String toString() {
        return "MinOrderCountCriterion{minOrderCount=" + minOrderCount + "}";
    }
}
