package com.firstclub.membership.strategy;

import com.firstclub.membership.enums.MatchPolicy;
import com.firstclub.membership.model.TierUpgradeCriteria;
import com.firstclub.membership.model.UserProfile;

import java.util.List;

public class CriteriaEvaluator {

    public boolean evaluate(TierUpgradeCriteria upgradeCriteria, UserProfile profile) {
        if (upgradeCriteria == null || upgradeCriteria.getCriteria() == null
                || upgradeCriteria.getCriteria().isEmpty()) {
            return true;
        }
        return evaluate(
                upgradeCriteria.getCriteria(),
                upgradeCriteria.getPolicy(),
                upgradeCriteria.getRequiredCount(),
                profile);
    }

    public boolean evaluate(List<Criterion> criteria, MatchPolicy policy, int requiredCount,
                            UserProfile profile) {
        long passingCount = criteria.stream()
                .filter(criterion -> criterion.matches(profile))
                .count();

        return switch (policy) {
            case ALL -> passingCount == criteria.size();
            case ANY -> passingCount >= 1;
            case AT_LEAST -> passingCount >= requiredCount;
        };
    }
}
