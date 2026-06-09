package com.firstclub.membership.service;

import com.firstclub.membership.enums.BenefitType;
import com.firstclub.membership.enums.MembershipPlanType;
import com.firstclub.membership.enums.TierType;
import com.firstclub.membership.exception.PlanNotFoundException;
import com.firstclub.membership.exception.TierNotFoundException;
import com.firstclub.membership.model.Benefit;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.model.TierUpgradeCriteria;
import com.firstclub.membership.repository.BenefitRepository;
import com.firstclub.membership.repository.MembershipPlanRepository;
import com.firstclub.membership.repository.MembershipTierRepository;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
public class MembershipCatalogService {

    private final MembershipPlanRepository planRepository;
    private final MembershipTierRepository tierRepository;
    private final BenefitRepository benefitRepository;

    public MembershipCatalogService(MembershipPlanRepository planRepository,
                                    MembershipTierRepository tierRepository,
                                    BenefitRepository benefitRepository) {
        this.planRepository = planRepository;
        this.tierRepository = tierRepository;
        this.benefitRepository = benefitRepository;
    }

    public MembershipPlan createPlan(MembershipPlanType planType, BigDecimal price) {
        MembershipPlan plan = new MembershipPlan(planType, price);
        planRepository.save(plan);
        log.info("Created plan planId={} planType={} price={}", plan.getId(), planType, price);
        return plan;
    }

    public MembershipTier createTier(TierType tierType, BigDecimal purchasePremium,
                                     TierUpgradeCriteria upgradeCriteria) {
        MembershipTier tier = new MembershipTier(tierType, purchasePremium, upgradeCriteria);
        tierRepository.save(tier);
        log.info("Created tier tierId={} tierType={}", tier.getId(), tierType);
        return tier;
    }

    public Benefit addBenefitToTier(Long tierId, BenefitType benefitType, BigDecimal value, String description) {
        MembershipTier tier = getTierById(tierId);
        Benefit benefit = new Benefit(tierId, benefitType, value, description);
        benefitRepository.save(benefit);
        tier.addBenefit(benefit);
        log.info("Added benefit to tier tierId={} benefitId={} benefitType={}",
                tierId, benefit.getId(), benefitType);
        return benefit;
    }

    public List<MembershipPlan> getPlans() {
        List<MembershipPlan> plans = planRepository.findAll();
        log.info("Fetched plans count={}", plans.size());
        return plans;
    }

    public List<MembershipTier> getTiers() {
        List<MembershipTier> tiers = tierRepository.findAllOrderedByRank();
        log.info("Fetched tiers ordered by rank count={}", tiers.size());
        return tiers;
    }

    public MembershipPlan getPlanById(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> {
                    log.warn("Plan not found planId={} exception={}", planId, PlanNotFoundException.class.getSimpleName());
                    return new PlanNotFoundException(planId);
                });
    }

    public MembershipTier getTierById(Long tierId) {
        return tierRepository.findById(tierId)
                .orElseThrow(() -> {
                    log.warn("Tier not found tierId={} exception={}", tierId, TierNotFoundException.class.getSimpleName());
                    return new TierNotFoundException(tierId);
                });
    }

    public List<Benefit> getBenefitsForTier(Long tierId) {
        MembershipTier tier = getTierById(tierId);
        log.info("Fetched benefits for tierId={} count={}", tierId, tier.getBenefits().size());
        return tier.getBenefits();
    }
}
