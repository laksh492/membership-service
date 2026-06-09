package com.firstclub.membership.service;

import com.firstclub.membership.exception.PlanNotFoundException;
import com.firstclub.membership.exception.TierNotFoundException;
import com.firstclub.membership.model.Benefit;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.repository.BenefitRepository;
import com.firstclub.membership.repository.MembershipPlanRepository;
import com.firstclub.membership.repository.MembershipTierRepository;
import lombok.extern.slf4j.Slf4j;

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

    public List<MembershipPlan> getActivePlans() {
        List<MembershipPlan> plans = planRepository.findAllActive();
        log.info("Fetched active plans count={}", plans.size());
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
        getTierById(tierId);
        List<Benefit> benefits = benefitRepository.findByTierId(tierId);
        log.info("Fetched benefits for tierId={} count={}", tierId, benefits.size());
        return benefits;
    }
}
