package com.firstclub.membership.service;

import com.firstclub.membership.dto.MembershipStatusResult;
import com.firstclub.membership.dto.SubscribeResult;
import com.firstclub.membership.enums.MembershipStatus;
import com.firstclub.membership.exception.ActiveMembershipExistsException;
import com.firstclub.membership.exception.InactivePlanException;
import com.firstclub.membership.exception.MembershipNotFoundException;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.model.UserMembership;
import com.firstclub.membership.repository.UserMembershipRepository;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MembershipSubscriptionService {

    // Per-userId lock prevents double-subscribe or concurrent state corruption in this demo.
    private static final ConcurrentHashMap<Long, Object> USER_LOCKS = new ConcurrentHashMap<>();

    private final UserMembershipRepository membershipRepository;
    private final MembershipCatalogService catalogService;
    private final PricingService pricingService;

    public MembershipSubscriptionService(UserMembershipRepository membershipRepository,
                                         MembershipCatalogService catalogService,
                                         PricingService pricingService) {
        this.membershipRepository = membershipRepository;
        this.catalogService = catalogService;
        this.pricingService = pricingService;
    }

    public SubscribeResult subscribe(Long userId, Long planId, Long tierId) {
        log.info("Subscribe start userId={} planId={} tierId={}", userId, planId, tierId);
        synchronized (lockFor(userId)) {
            membershipRepository.findActiveByUserId(userId).ifPresent(m -> {
                log.warn("Subscribe rejected userId={} exception={}", userId,
                        ActiveMembershipExistsException.class.getSimpleName());
                throw new ActiveMembershipExistsException(userId);
            });

            MembershipPlan plan = catalogService.getPlanById(planId);
            if (!plan.isActive()) {
                log.warn("Subscribe rejected userId={} planId={} exception={}", userId, planId,
                        InactivePlanException.class.getSimpleName());
                throw new InactivePlanException(planId);
            }

            MembershipTier tier = catalogService.getTierById(tierId);
            BigDecimal priceCharged = pricingService.calculateSubscribePrice(plan, tier);

            LocalDateTime now = LocalDateTime.now();
            UserMembership membership = new UserMembership();
            membership.setUserId(userId);
            membership.setPlanId(planId);
            membership.setCurrentTierId(tierId);
            membership.setStatus(MembershipStatus.ACTIVE);
            membership.setStartDate(now);
            membership.setExpiryDate(now.plusDays(plan.getPlanType().getDurationDays()));
            membershipRepository.save(membership);

            SubscribeResult result = new SubscribeResult();
            result.setMembershipId(membership.getId());
            result.setUserId(userId);
            result.setPlanId(planId);
            result.setTierId(tierId);
            result.setPriceCharged(priceCharged);
            result.setStatus(membership.getStatus());
            result.setExpiryDate(membership.getExpiryDate());

            log.info("Subscribe success userId={} membershipId={} planId={} tierId={} priceCharged={} expiryDate={}",
                    userId, membership.getId(), planId, tierId, priceCharged, membership.getExpiryDate());
            return result;
        }
    }

    public void cancel(Long userId) {
        log.info("Cancel start userId={}", userId);
        synchronized (lockFor(userId)) {
            UserMembership membership = membershipRepository.findActiveByUserId(userId)
                    .orElseThrow(() -> {
                        log.warn("Cancel rejected userId={} exception={}", userId,
                                MembershipNotFoundException.class.getSimpleName());
                        return new MembershipNotFoundException(userId);
                    });

            membership.setStatus(MembershipStatus.CANCELLED);
            membership.setCancelledAt(LocalDateTime.now());

            log.info("Cancel success userId={} membershipId={}", userId, membership.getId());
        }
    }

    public MembershipStatusResult getMembershipStatus(Long userId) {
        log.info("Get membership status start userId={}", userId);
        UserMembership membership = findLatestMembershipForUser(userId)
                .orElseThrow(() -> {
                    log.warn("Status lookup rejected userId={} exception={}", userId,
                            MembershipNotFoundException.class.getSimpleName());
                    return new MembershipNotFoundException(userId);
                });

        LocalDateTime now = LocalDateTime.now();
        if (membership.isActive() && membership.isExpired(now)) {
            membership.setStatus(MembershipStatus.EXPIRED);
            log.info("Lazy-expired membership userId={} membershipId={}", userId, membership.getId());
        }

        long daysRemaining = 0;
        if (membership.getExpiryDate() != null && membership.getStatus() == MembershipStatus.ACTIVE) {
            daysRemaining = Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(),
                    membership.getExpiryDate().toLocalDate()));
        }

        MembershipStatusResult result = new MembershipStatusResult();
        result.setUserId(userId);
        result.setMembershipId(membership.getId());
        result.setPlanId(membership.getPlanId());
        result.setTierId(membership.getCurrentTierId());
        result.setStatus(membership.getStatus());
        result.setExpiryDate(membership.getExpiryDate());
        result.setDaysRemaining(daysRemaining);
        result.setActive(membership.getStatus() == MembershipStatus.ACTIVE);

        log.info("Get membership status success userId={} membershipId={} status={} daysRemaining={}",
                userId, membership.getId(), membership.getStatus(), daysRemaining);
        return result;
    }

    private java.util.Optional<UserMembership> findLatestMembershipForUser(Long userId) {
        return membershipRepository.findAll().stream()
                .filter(m -> userId.equals(m.getUserId()))
                .max(Comparator.comparing(UserMembership::getId));
    }

    private static Object lockFor(Long userId) {
        return USER_LOCKS.computeIfAbsent(userId, id -> new Object());
    }
}
