package com.firstclub.membership.model;

import com.firstclub.membership.enums.MatchPolicy;
import com.firstclub.membership.strategy.Criterion;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TierUpgradeCriteria {
    private List<Criterion> criteria = new ArrayList<>();
    private MatchPolicy policy;
    private int requiredCount;

    @Override
    public String toString() {
        return "TierUpgradeCriteria{criteria=" + criteria + ", policy=" + policy
                + ", requiredCount=" + requiredCount + "}";
    }
}
