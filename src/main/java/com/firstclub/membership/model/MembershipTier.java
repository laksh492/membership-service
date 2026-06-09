package com.firstclub.membership.model;

import com.firstclub.membership.enums.TierType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public class MembershipTier {
    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    private Long id;
    private TierType tierType;
    private BigDecimal purchasePremium;
    private List<Benefit> benefits = new ArrayList<>();
    private TierUpgradeCriteria upgradeCriteria;

    public MembershipTier(TierType tierType, BigDecimal purchasePremium, TierUpgradeCriteria upgradeCriteria) {
        this.id = ID_COUNTER.incrementAndGet();
        this.tierType = tierType;
        this.purchasePremium = purchasePremium;
        this.upgradeCriteria = upgradeCriteria;
    }

    public void addBenefit(Benefit benefit) {
        this.benefits.add(benefit);
    }

    @Override
    public String toString() {
        return "MembershipTier{id=" + id + ", tierType=" + tierType + ", purchasePremium="
                + purchasePremium + ", benefits=" + benefits + ", upgradeCriteria="
                + upgradeCriteria + "}";
    }
}
