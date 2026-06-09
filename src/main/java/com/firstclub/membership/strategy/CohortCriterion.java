package com.firstclub.membership.strategy;

import com.firstclub.membership.model.UserProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CohortCriterion implements Criterion {
    private final String cohort;

    @Override
    public boolean matches(UserProfile profile) {
        return cohort.equals(profile.getCohort());
    }

    @Override
    public String toString() {
        return "CohortCriterion{cohort='" + cohort + "'}";
    }
}
