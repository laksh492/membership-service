package com.firstclub.membership.service;

import com.firstclub.membership.enums.MembershipStatus;
import com.firstclub.membership.exception.ActiveMembershipExistsException;
import com.firstclub.membership.exception.AlreadyOnTierException;
import com.firstclub.membership.exception.InvalidTierChangeException;
import com.firstclub.membership.exception.MembershipNotFoundException;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.model.UserMembership;
import com.firstclub.membership.model.UserProfile;
import com.firstclub.membership.repository.UserMembershipRepository;
import com.firstclub.membership.strategy.CriteriaEvaluator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MembershipSubscriptionService {

    // Per-userId lock prevents double-subscribe or concurrent state corruption in this demo.
    private static final ConcurrentHashMap<Long, Object> USER_LOCKS = new ConcurrentHashMap<>();

    private final UserMembershipRepository membershipRepository;
    private final MembershipCatalogService catalogService;
    private final PricingService pricingService;
    private final UserProfileService profileService;
    private final CriteriaEvaluator criteriaEvaluator;

    public MembershipSubscriptionService(UserMembershipRepository membershipRepository,
                                         MembershipCatalogService catalogService,
                                         PricingService pricingService,
                                         UserProfileService profileService,
                                         CriteriaEvaluator criteriaEvaluator) {
        this.membershipRepository = membershipRepository;
        this.catalogService = catalogService;
        this.pricingService = pricingService;
        this.profileService = profileService;
        this.criteriaEvaluator = criteriaEvaluator;
    }

    public UserMembership subscribe(Long userId, Long planId, Long tierId) {
        log.info("Subscribe requested userId={} planId={} tierId={}", userId, planId, tierId);
        synchronized (lockFor(userId)) {
            membershipRepository.findActiveByUserId(userId).ifPresent(m -> {
                log.warn("Subscribe rejected userId={} existingMembershipId={} reason={}",
                        userId, m.getId(), ActiveMembershipExistsException.class.getSimpleName());
                throw new ActiveMembershipExistsException(userId);
            });

            MembershipPlan plan = catalogService.getPlanById(planId);
            MembershipTier tier = catalogService.getTierById(tierId);
            BigDecimal priceCharged = pricingService.calculateSubscribePrice(plan, tier);

            LocalDateTime now = LocalDateTime.now();
            UserMembership membership = new UserMembership(
                    userId,
                    planId,
                    tierId,
                    now,
                    now.plusDays(plan.getPlanType().getDurationDays()),
                    priceCharged);
            membershipRepository.save(membership);

            log.info("Subscribe completed userId={} membership={} priceCharged={}",
                    userId, membership, priceCharged);
            return membership;
        }
    }

    public UserMembership cancel(Long userId) {
        log.info("Cancel requested userId={}", userId);
        synchronized (lockFor(userId)) {
            UserMembership membership = membershipRepository.findActiveByUserId(userId)
                    .orElseThrow(() -> {
                        log.warn("Cancel rejected userId={} reason={}",
                                userId, MembershipNotFoundException.class.getSimpleName());
                        return new MembershipNotFoundException(userId);
                    });

            membership.setStatus(MembershipStatus.CANCELLED);
            membership.setCancelledAt(LocalDateTime.now());

            log.info("Cancel completed userId={} membership={}", userId, membership);
            return membership;
        }
    }

    public UserMembership getMembershipStatus(Long userId) {
        log.info("Membership status requested userId={}", userId);
        UserMembership membership = membershipRepository.findLatestByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Membership status rejected userId={} reason={}",
                            userId, MembershipNotFoundException.class.getSimpleName());
                    return new MembershipNotFoundException(userId);
                });

        LocalDateTime now = LocalDateTime.now();
        if (membership.isActive() && membership.isExpired(now)) {
            membership.setStatus(MembershipStatus.EXPIRED);
            log.info("Membership lazy-expired on status read userId={} membershipId={} expiryDate={}",
                    userId, membership.getId(), membership.getExpiryDate());
        }

        log.info("Membership status returned userId={} membership={} daysRemaining={}",
                userId, membership, membership.daysRemaining(now));
        return membership;
    }

    public UserMembership upgradeTier(Long userId, Long targetTierId) {
        log.info("Upgrade requested userId={} targetTierId={}", userId, targetTierId);
        synchronized (lockFor(userId)) {
            UserMembership currentMembership = requireActiveMembership(userId);
            MembershipTier currentTier = catalogService.getTierById(currentMembership.getCurrentTierId());
            MembershipTier targetTier = catalogService.getTierById(targetTierId);

            if (currentTier.getId().equals(targetTierId)) {
                log.warn("Upgrade rejected userId={} membershipId={} tierId={} reason={}",
                        userId, currentMembership.getId(), targetTierId,
                        AlreadyOnTierException.class.getSimpleName());
                throw new AlreadyOnTierException(targetTierId);
            }
            if (targetTier.getTierType().getRank() <= currentTier.getTierType().getRank()) {
                log.warn("Upgrade rejected userId={} membershipId={} currentTier={} targetTier={} reason={}",
                        userId, currentMembership.getId(), currentTier.getTierType(), targetTier.getTierType(),
                        InvalidTierChangeException.class.getSimpleName());
                throw new InvalidTierChangeException(
                        "Target tier rank must be higher than current tier rank");
            }

            UserProfile profile = profileService.getUser(userId);
            boolean criteriaMet = criteriaEvaluator.evaluate(targetTier.getUpgradeCriteria(), profile);
            BigDecimal amountCharged = criteriaMet
                    ? BigDecimal.ZERO
                    : pricingService.calculateUpgradeCharge(currentTier, targetTier);

            UserMembership newMembership = replaceMembershipTier(
                    currentMembership, targetTierId, amountCharged);

            log.info("Upgrade completed userId={} previousMembershipId={} newMembership={} criteriaMet={} amountCharged={}",
                    userId, currentMembership.getId(), newMembership, criteriaMet, amountCharged);
            return newMembership;
        }
    }

    public UserMembership downgradeTier(Long userId, Long targetTierId) {
        log.info("Downgrade requested userId={} targetTierId={}", userId, targetTierId);
        synchronized (lockFor(userId)) {
            UserMembership currentMembership = requireActiveMembership(userId);
            MembershipTier currentTier = catalogService.getTierById(currentMembership.getCurrentTierId());
            MembershipTier targetTier = catalogService.getTierById(targetTierId);

            if (currentTier.getId().equals(targetTierId)) {
                log.warn("Downgrade rejected userId={} membershipId={} tierId={} reason={}",
                        userId, currentMembership.getId(), targetTierId,
                        AlreadyOnTierException.class.getSimpleName());
                throw new AlreadyOnTierException(targetTierId);
            }
            if (targetTier.getTierType().getRank() >= currentTier.getTierType().getRank()) {
                log.warn("Downgrade rejected userId={} membershipId={} currentTier={} targetTier={} reason={}",
                        userId, currentMembership.getId(), currentTier.getTierType(), targetTier.getTierType(),
                        InvalidTierChangeException.class.getSimpleName());
                throw new InvalidTierChangeException(
                        "Target tier rank must be lower than current tier rank");
            }

            UserMembership newMembership = replaceMembershipTier(
                    currentMembership, targetTierId, BigDecimal.ZERO);

            log.info("Downgrade completed userId={} previousMembershipId={} newMembership={}",
                    userId, currentMembership.getId(), newMembership);
            return newMembership;
        }
    }

    private UserMembership replaceMembershipTier(UserMembership currentMembership, Long targetTierId,
                                                 BigDecimal amountPaid) {
        LocalDateTime now = LocalDateTime.now();
        currentMembership.setStatus(MembershipStatus.CANCELLED);
        currentMembership.setCancelledAt(now);

        UserMembership newMembership = new UserMembership(
                currentMembership.getUserId(),
                currentMembership.getPlanId(),
                targetTierId,
                now,
                currentMembership.getExpiryDate(),
                amountPaid);
        membershipRepository.save(newMembership);
        return newMembership;
    }

    private UserMembership requireActiveMembership(Long userId) {
        return membershipRepository.findActiveByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Membership operation rejected userId={} reason={}",
                            userId, MembershipNotFoundException.class.getSimpleName());
                    return new MembershipNotFoundException(userId);
                });
    }

    private static Object lockFor(Long userId) {
        return USER_LOCKS.computeIfAbsent(userId, id -> new Object());
    }
}
