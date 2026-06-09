package com.firstclub.membership.service;

import com.firstclub.membership.exception.InvalidPricingException;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipTier;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class PricingService {

    public BigDecimal calculateSubscribePrice(MembershipPlan plan, MembershipTier tier) {
        if (plan == null || tier == null) {
            throw new InvalidPricingException("Plan and tier are required for subscribe pricing");
        }
        if (plan.getPrice() == null || tier.getPurchasePremium() == null) {
            throw new InvalidPricingException("Plan price and tier premium must not be null");
        }
        BigDecimal price = plan.getPrice().add(tier.getPurchasePremium());
        log.debug("Subscribe price calculated planId={} tierId={} price={}", plan.getId(), tier.getId(), price);
        return price;
    }

    public BigDecimal calculateUpgradeCharge(MembershipTier current, MembershipTier target) {
        if (current == null || target == null) {
            throw new InvalidPricingException("Current and target tiers are required for upgrade pricing");
        }
        if (current.getPurchasePremium() == null || target.getPurchasePremium() == null) {
            throw new InvalidPricingException("Tier premiums must not be null");
        }
        BigDecimal delta = target.getPurchasePremium().subtract(current.getPurchasePremium());
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPricingException(
                    "Target tier premium (" + target.getPurchasePremium()
                            + ") is lower than current tier premium (" + current.getPurchasePremium() + ")");
        }
        log.debug("Upgrade charge calculated currentTierId={} targetTierId={} delta={}",
                current.getId(), target.getId(), delta);
        return delta;
    }
}
