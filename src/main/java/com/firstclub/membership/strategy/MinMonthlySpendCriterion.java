package com.firstclub.membership.strategy;

import com.firstclub.membership.model.UserProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class MinMonthlySpendCriterion implements Criterion {
    private final BigDecimal minMonthlySpend;

    @Override
    public boolean matches(UserProfile profile) {
        return profile.getMonthlyOrderValue().compareTo(minMonthlySpend) >= 0;
    }

    @Override
    public String toString() {
        return "MinMonthlySpendCriterion{minMonthlySpend=" + minMonthlySpend + "}";
    }
}
