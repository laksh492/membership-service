package com.firstclub.membership.service;

import com.firstclub.membership.dto.DowngradeResult;
import com.firstclub.membership.dto.UpgradeResult;
import com.firstclub.membership.enums.MembershipStatus;
import com.firstclub.membership.exception.AlreadyOnTierException;
import com.firstclub.membership.exception.InvalidTierChangeException;
import com.firstclub.membership.exception.MembershipCancelledException;
import com.firstclub.membership.exception.MembershipExpiredException;
import com.firstclub.membership.exception.MembershipNotFoundException;
import com.firstclub.membership.exception.TierCriteriaNotMetException;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.model.UserMembership;
import com.firstclub.membership.model.UserProfile;
import com.firstclub.membership.repository.UserMembershipRepository;
import com.firstclub.membership.strategy.CriteriaEvaluator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MembershipTierChangeService {

    // Per-userId lock prevents concurrent tier corruption in this demo.
    private static final ConcurrentHashMap<Long, Object> USER_LOCKS = new ConcurrentHashMap<>();

    private final UserMembershipRepository membershipRepository;
    private final MembershipCatalogService catalogService;
    private final PricingService pricingService;
    private final UserProfileService profileService;
    private final CriteriaEvaluator criteriaEvaluator;

    public MembershipTierChangeService(UserMembershipRepository membershipRepository,
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

    public UpgradeResult upgradeTier(Long userId, Long targetTierId) {
        log.info("Upgrade start userId={} targetTierId={}", userId, targetTierId);
        synchronized (lockFor(userId)) {
            UserMembership membership = requireActiveMembership(userId);
            MembershipTier currentTier = catalogService.getTierById(membership.getCurrentTierId());
            MembershipTier targetTier = catalogService.getTierById(targetTierId);

            if (currentTier.getId().equals(targetTierId)) {
                log.warn("Upgrade rejected userId={} tierId={} exception={}", userId, targetTierId,
                        AlreadyOnTierException.class.getSimpleName());
                throw new AlreadyOnTierException(targetTierId);
            }
            if (targetTier.getTierType().getRank() <= currentTier.getTierType().getRank()) {
                log.warn("Upgrade rejected userId={} targetTierId={} exception={}", userId, targetTierId,
                        InvalidTierChangeException.class.getSimpleName());
                throw new InvalidTierChangeException(
                        "Target tier rank must be higher than current tier rank");
            }

            UserProfile profile = profileService.getOrCreate(userId);
            boolean criteriaMet = criteriaEvaluator.evaluate(targetTier.getUpgradeCriteria(), profile);
            BigDecimal amountCharged = criteriaMet
                    ? BigDecimal.ZERO
                    : pricingService.calculateUpgradeCharge(currentTier, targetTier);

            Long previousTierId = membership.getCurrentTierId();
            membership.setCurrentTierId(targetTierId);

            UpgradeResult result = new UpgradeResult();
            result.setMembershipId(membership.getId());
            result.setUserId(userId);
            result.setPreviousTierId(previousTierId);
            result.setNewTierId(targetTierId);
            result.setAmountCharged(amountCharged);
            result.setCriteriaMet(criteriaMet);
            result.setFreeUpgrade(criteriaMet);

            log.info("Upgrade success userId={} membershipId={} previousTierId={} newTierId={} "
                            + "criteriaMet={} amountCharged={}",
                    userId, membership.getId(), previousTierId, targetTierId, criteriaMet, amountCharged);
            return result;
        }
    }

    public DowngradeResult downgradeTier(Long userId, Long targetTierId) {
        log.info("Downgrade start userId={} targetTierId={}", userId, targetTierId);
        synchronized (lockFor(userId)) {
            UserMembership membership = requireActiveMembership(userId);
            MembershipTier currentTier = catalogService.getTierById(membership.getCurrentTierId());
            MembershipTier targetTier = catalogService.getTierById(targetTierId);

            if (currentTier.getId().equals(targetTierId)) {
                log.warn("Downgrade rejected userId={} tierId={} exception={}", userId, targetTierId,
                        AlreadyOnTierException.class.getSimpleName());
                throw new AlreadyOnTierException(targetTierId);
            }
            if (targetTier.getTierType().getRank() >= currentTier.getTierType().getRank()) {
                log.warn("Downgrade rejected userId={} targetTierId={} exception={}", userId, targetTierId,
                        InvalidTierChangeException.class.getSimpleName());
                throw new InvalidTierChangeException(
                        "Target tier rank must be lower than current tier rank");
            }

            Long previousTierId = membership.getCurrentTierId();
            membership.setCurrentTierId(targetTierId);

            DowngradeResult result = new DowngradeResult();
            result.setMembershipId(membership.getId());
            result.setUserId(userId);
            result.setPreviousTierId(previousTierId);
            result.setNewTierId(targetTierId);

            log.info("Downgrade success userId={} membershipId={} previousTierId={} newTierId={}",
                    userId, membership.getId(), previousTierId, targetTierId);
            return result;
        }
    }

    public UpgradeResult evaluateAndUpgrade(Long userId) {
        log.info("Evaluate and upgrade start userId={}", userId);
        synchronized (lockFor(userId)) {
            UserMembership membership = requireActiveMembership(userId);
            MembershipTier currentTier = catalogService.getTierById(membership.getCurrentTierId());

            List<MembershipTier> tiers = catalogService.getTiers();
            MembershipTier nextTier = tiers.stream()
                    .filter(t -> t.getTierType().getRank() == currentTier.getTierType().getRank() + 1)
                    .findFirst()
                    .orElseThrow(() -> {
                        log.warn("Evaluate and upgrade rejected userId={} exception={}", userId,
                                InvalidTierChangeException.class.getSimpleName());
                        return new InvalidTierChangeException("No higher tier available for upgrade");
                    });

            UserProfile profile = profileService.getOrCreate(userId);
            boolean criteriaMet = criteriaEvaluator.evaluate(nextTier.getUpgradeCriteria(), profile);
            if (!criteriaMet) {
                log.warn("Evaluate and upgrade rejected userId={} targetTierId={} exception={}",
                        userId, nextTier.getId(), TierCriteriaNotMetException.class.getSimpleName());
                throw new TierCriteriaNotMetException(nextTier.getId());
            }

            Long previousTierId = membership.getCurrentTierId();
            membership.setCurrentTierId(nextTier.getId());

            UpgradeResult result = new UpgradeResult();
            result.setMembershipId(membership.getId());
            result.setUserId(userId);
            result.setPreviousTierId(previousTierId);
            result.setNewTierId(nextTier.getId());
            result.setAmountCharged(BigDecimal.ZERO);
            result.setCriteriaMet(true);
            result.setFreeUpgrade(true);

            log.info("Evaluate and upgrade success userId={} membershipId={} previousTierId={} newTierId={}",
                    userId, membership.getId(), previousTierId, nextTier.getId());
            return result;
        }
    }

    private UserMembership requireActiveMembership(Long userId) {
        UserMembership membership = membershipRepository.findAll().stream()
                .filter(m -> userId.equals(m.getUserId()))
                .max(Comparator.comparing(UserMembership::getId))
                .orElseThrow(() -> {
                    log.warn("Tier change rejected userId={} exception={}", userId,
                            MembershipNotFoundException.class.getSimpleName());
                    return new MembershipNotFoundException(userId);
                });

        if (membership.getStatus() == MembershipStatus.CANCELLED) {
            log.warn("Tier change rejected userId={} membershipId={} exception={}", userId,
                    membership.getId(), MembershipCancelledException.class.getSimpleName());
            throw new MembershipCancelledException(membership.getId());
        }

        LocalDateTime now = LocalDateTime.now();
        if (membership.getStatus() == MembershipStatus.EXPIRED
                || (membership.isActive() && membership.isExpired(now))) {
            if (membership.isActive() && membership.isExpired(now)) {
                membership.setStatus(MembershipStatus.EXPIRED);
            }
            log.warn("Tier change rejected userId={} membershipId={} exception={}", userId,
                    membership.getId(), MembershipExpiredException.class.getSimpleName());
            throw new MembershipExpiredException(membership.getId());
        }

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            log.warn("Tier change rejected userId={} membershipId={} exception={}", userId,
                    membership.getId(), MembershipNotFoundException.class.getSimpleName());
            throw new MembershipNotFoundException(userId);
        }

        return membership;
    }

    private static Object lockFor(Long userId) {
        return USER_LOCKS.computeIfAbsent(userId, id -> new Object());
    }
}
