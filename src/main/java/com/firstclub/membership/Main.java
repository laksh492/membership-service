package com.firstclub.membership;

import com.firstclub.membership.enums.BenefitType;
import com.firstclub.membership.enums.MatchPolicy;
import com.firstclub.membership.enums.MembershipPlanType;
import com.firstclub.membership.enums.MembershipStatus;
import com.firstclub.membership.enums.TierType;
import com.firstclub.membership.exception.ActiveMembershipExistsException;
import com.firstclub.membership.exception.AlreadyOnTierException;
import com.firstclub.membership.exception.InvalidTierChangeException;
import com.firstclub.membership.exception.MembershipNotFoundException;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.model.TierUpgradeCriteria;
import com.firstclub.membership.model.UserMembership;
import com.firstclub.membership.repository.MembershipPlanRepository;
import com.firstclub.membership.repository.MembershipTierRepository;
import com.firstclub.membership.repository.UserMembershipRepository;
import com.firstclub.membership.repository.UserProfileRepository;
import com.firstclub.membership.repository.impl.InMemoryBenefitRepository;
import com.firstclub.membership.repository.impl.InMemoryMembershipPlanRepository;
import com.firstclub.membership.repository.impl.InMemoryMembershipTierRepository;
import com.firstclub.membership.repository.impl.InMemoryUserMembershipRepository;
import com.firstclub.membership.repository.impl.InMemoryUserProfileRepository;
import com.firstclub.membership.service.MembershipCatalogService;
import com.firstclub.membership.service.MembershipSubscriptionService;
import com.firstclub.membership.service.PricingService;
import com.firstclub.membership.service.UserProfileService;
import com.firstclub.membership.strategy.CohortCriterion;
import com.firstclub.membership.strategy.CriteriaEvaluator;
import com.firstclub.membership.strategy.MinMonthlySpendCriterion;
import com.firstclub.membership.strategy.MinOrderCountCriterion;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
public class Main {

    static MembershipPlanRepository planRepository;
    static MembershipTierRepository tierRepository;
    static UserMembershipRepository membershipRepository;
    static UserProfileRepository profileRepository;

    static PricingService pricingService;
    static MembershipCatalogService catalogService;
    static UserProfileService profileService;
    static MembershipSubscriptionService subscriptionService;

    static Long monthlyPlanId;
    static Long silverTierId;
    static Long goldTierId;
    static Long platinumTierId;

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        initializeDataLayer();
        runAllTests();
        log.info("Test run complete passed={} failed={}", testsPassed, testsFailed);
        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    static void initializeDataLayer() {
        planRepository = new InMemoryMembershipPlanRepository();
        tierRepository = new InMemoryMembershipTierRepository();
        membershipRepository = new InMemoryUserMembershipRepository();
        profileRepository = new InMemoryUserProfileRepository();

        pricingService = new PricingService();
        catalogService = new MembershipCatalogService(
                planRepository, tierRepository, new InMemoryBenefitRepository());
        profileService = new UserProfileService(profileRepository);
        subscriptionService = new MembershipSubscriptionService(
                membershipRepository, catalogService, pricingService, profileService, new CriteriaEvaluator());

        seedPlans();
        seedTiersAndBenefits();
        seedUserProfiles();

        log.info("Data layer initialized plans={} tiers={} profiles={}",
                planRepository.findAll().size(), tierRepository.findAll().size(),
                profileRepository.findAll().size());
    }

    private static void seedPlans() {
        monthlyPlanId = catalogService.createPlan(MembershipPlanType.MONTHLY, new BigDecimal("299")).getId();
        catalogService.createPlan(MembershipPlanType.QUARTERLY, new BigDecimal("799"));
        catalogService.createPlan(MembershipPlanType.YEARLY, new BigDecimal("2499"));
    }

    private static void seedTiersAndBenefits() {
        silverTierId = catalogService.createTier(TierType.SILVER, BigDecimal.ZERO, null).getId();
        catalogService.addBenefitToTier(silverTierId, BenefitType.EXCLUSIVE_DEALS, BigDecimal.ZERO,
                "Access to member-only deals");

        TierUpgradeCriteria goldCriteria = new TierUpgradeCriteria();
        goldCriteria.setPolicy(MatchPolicy.AT_LEAST);
        goldCriteria.setRequiredCount(2);
        goldCriteria.getCriteria().add(new MinOrderCountCriterion(5));
        goldCriteria.getCriteria().add(new MinMonthlySpendCriterion(new BigDecimal("5000")));
        goldCriteria.getCriteria().add(new CohortCriterion("VIP"));
        goldTierId = catalogService.createTier(TierType.GOLD, new BigDecimal("100"), goldCriteria).getId();
        catalogService.addBenefitToTier(goldTierId, BenefitType.EXTRA_DISCOUNT, new BigDecimal("10"),
                "10% extra discount on orders");

        TierUpgradeCriteria platinumCriteria = new TierUpgradeCriteria();
        platinumCriteria.setPolicy(MatchPolicy.ALL);
        platinumCriteria.getCriteria().add(new MinOrderCountCriterion(15));
        platinumCriteria.getCriteria().add(new MinMonthlySpendCriterion(new BigDecimal("15000")));
        platinumTierId = catalogService.createTier(TierType.PLATINUM, new BigDecimal("300"), platinumCriteria).getId();
        catalogService.addBenefitToTier(platinumTierId, BenefitType.FREE_DELIVERY, BigDecimal.ZERO,
                "Free delivery on all orders");
        catalogService.addBenefitToTier(platinumTierId, BenefitType.PRIORITY_SUPPORT, BigDecimal.ZERO,
                "Priority customer support");
    }

    private static void seedUserProfiles() {
        profileService.createUser(1L, null);
        profileService.createUser(2L, "VIP");
        profileService.updateOrderStats(2L, 10, new BigDecimal("8000"));
        profileService.createUser(3L, null);
    }

    static void runAllTests() {
        runTest("testGetPlansAndTiers", Main::testGetPlansAndTiers);
        runTest("testSubscribeMonthlySilver", Main::testSubscribeMonthlySilver);
        runTest("testSubscribeDuplicateRejected", Main::testSubscribeDuplicateRejected);
        runTest("testGetMembershipStatus", Main::testGetMembershipStatus);
        runTest("testCancelMembership", Main::testCancelMembership);
        runTest("testUpgradeAfterCancelRejected", Main::testUpgradeAfterCancelRejected);
        runTest("testPaidUpgradeGold", Main::testPaidUpgradeGold);
        runTest("testFreeUpgradeGold", Main::testFreeUpgradeGold);
        runTest("testDowngradeToSilver", Main::testDowngradeToSilver);
        runTest("testUpgradeToSameTierRejected", Main::testUpgradeToSameTierRejected);
        runTest("testDowngradeToHigherTierRejected", Main::testDowngradeToHigherTierRejected);
        runTest("testUpgradeFreeWhenCriteriaMet", Main::testUpgradeFreeWhenCriteriaMet);
        runTest("testUpgradePaidWhenCriteriaNotMet", Main::testUpgradePaidWhenCriteriaNotMet);
        runTest("testMembershipLazyExpire", Main::testMembershipLazyExpire);
    }

    private static void runTest(String name, Runnable test) {
        try {
            test.run();
            testsPassed++;
            log.info("PASS {}", name);
        } catch (AssertionError e) {
            testsFailed++;
            log.error("FAIL {} - {}", name, e.getMessage());
        }
    }

    static void testGetPlansAndTiers() {
        List<MembershipPlan> plans = catalogService.getPlans();
        assertEquals(3, plans.size(), "Plan count");

        List<MembershipTier> tiers = catalogService.getTiers();
        assertEquals(3, tiers.size(), "Tier count");
        assertEquals(TierType.SILVER, tiers.get(0).getTierType(), "First tier should be Silver");
        assertEquals(TierType.PLATINUM, tiers.get(2).getTierType(), "Last tier should be Platinum");
    }

    static void testSubscribeMonthlySilver() {
        UserMembership membership = subscriptionService.subscribe(1L, monthlyPlanId, silverTierId);
        BigDecimal expectedPrice = pricingService.calculateSubscribePrice(
                catalogService.getPlanById(monthlyPlanId), catalogService.getTierById(silverTierId));
        assertEquals(new BigDecimal("299"), expectedPrice, "Subscribe price");
        assertEquals(new BigDecimal("299"), membership.getAmountPaid(), "Subscribe amount paid");
        assertEquals(MembershipStatus.ACTIVE, membership.getStatus(), "Membership status");
        assertEquals(silverTierId, membership.getCurrentTierId(), "Subscribed tier");
        assertTrue(membership.getExpiryDate() != null, "Expiry date should be set");
    }

    static void testSubscribeDuplicateRejected() {
        assertThrows(ActiveMembershipExistsException.class,
                () -> subscriptionService.subscribe(1L, monthlyPlanId, silverTierId),
                "Duplicate subscribe should be rejected");
    }

    static void testGetMembershipStatus() {
        UserMembership membership = subscriptionService.getMembershipStatus(1L);
        assertEquals(MembershipStatus.ACTIVE, membership.getStatus(), "Status should be ACTIVE");
        assertEquals(monthlyPlanId, membership.getPlanId(), "Plan id");
        assertEquals(silverTierId, membership.getCurrentTierId(), "Tier id");
        assertTrue(membership.daysRemaining(LocalDateTime.now()) > 0, "Days remaining should be positive");
        assertTrue(membership.isActive(), "Should be active");
    }

    static void testCancelMembership() {
        UserMembership cancelled = subscriptionService.cancel(1L);
        assertEquals(MembershipStatus.CANCELLED, cancelled.getStatus(), "Status should be CANCELLED");
        UserMembership membership = subscriptionService.getMembershipStatus(1L);
        assertEquals(MembershipStatus.CANCELLED, membership.getStatus(), "Status should be CANCELLED");
    }

    static void testUpgradeAfterCancelRejected() {
        assertThrows(MembershipNotFoundException.class,
                () -> subscriptionService.upgradeTier(1L, goldTierId),
                "Upgrade after cancel should be rejected (no active membership)");
    }

    static void testPaidUpgradeGold() {
        subscriptionService.subscribe(3L, monthlyPlanId, silverTierId);
        UserMembership membership = subscriptionService.upgradeTier(3L, goldTierId);
        BigDecimal expectedCharge = pricingService.calculateUpgradeCharge(
                catalogService.getTierById(silverTierId), catalogService.getTierById(goldTierId));
        assertEquals(new BigDecimal("100"), expectedCharge, "Paid upgrade charge");
        assertEquals(new BigDecimal("100"), membership.getAmountPaid(), "Upgrade amount paid");
        assertFalse(new CriteriaEvaluator().evaluate(
                catalogService.getTierById(goldTierId).getUpgradeCriteria(),
                profileService.getUser(3L)), "Criteria should not be met");
        assertEquals(goldTierId, membership.getCurrentTierId(), "New tier should be Gold");
    }

    static void testFreeUpgradeGold() {
        subscriptionService.subscribe(4L, monthlyPlanId, silverTierId);
        profileService.createUser(4L, null);
        profileService.updateOrderStats(4L, 5, new BigDecimal("6000"));
        UserMembership membership = subscriptionService.upgradeTier(4L, goldTierId);
        assertTrue(new CriteriaEvaluator().evaluate(
                catalogService.getTierById(goldTierId).getUpgradeCriteria(),
                profileService.getUser(4L)), "Criteria should be met");
        assertEquals(goldTierId, membership.getCurrentTierId(), "New tier should be Gold");
    }

    static void testDowngradeToSilver() {
        UserMembership downgraded = subscriptionService.downgradeTier(3L, silverTierId);
        assertEquals(silverTierId, downgraded.getCurrentTierId(), "Tier should be Silver after downgrade");
        UserMembership membership = subscriptionService.getMembershipStatus(3L);
        assertEquals(silverTierId, membership.getCurrentTierId(), "Tier should be Silver after downgrade");
    }

    static void testUpgradeToSameTierRejected() {
        assertThrows(AlreadyOnTierException.class,
                () -> subscriptionService.upgradeTier(3L, silverTierId),
                "Upgrade to same tier should be rejected");
    }

    static void testDowngradeToHigherTierRejected() {
        assertThrows(InvalidTierChangeException.class,
                () -> subscriptionService.downgradeTier(3L, goldTierId),
                "Downgrade to higher tier should be rejected");
    }

    static void testUpgradeFreeWhenCriteriaMet() {
        subscriptionService.subscribe(5L, monthlyPlanId, silverTierId);
        profileService.createUser(5L, null);
        profileService.updateOrderStats(5L, 5, new BigDecimal("6000"));
        UserMembership membership = subscriptionService.upgradeTier(5L, goldTierId);
        assertEquals(goldTierId, membership.getCurrentTierId(), "Should upgrade to Gold");
        assertEquals(BigDecimal.ZERO, membership.getAmountPaid(), "Upgrade should be free when criteria met");
    }

    static void testUpgradePaidWhenCriteriaNotMet() {
        subscriptionService.subscribe(6L, monthlyPlanId, silverTierId);
        profileService.createUser(6L, null);
        UserMembership membership = subscriptionService.upgradeTier(6L, goldTierId);
        assertEquals(goldTierId, membership.getCurrentTierId(), "Should upgrade to Gold");
        assertEquals(new BigDecimal("100"), membership.getAmountPaid(), "Upgrade should be paid when criteria not met");
    }

    static void testMembershipLazyExpire() {
        UserMembership expired = new UserMembership(
                7L, monthlyPlanId, silverTierId,
                LocalDateTime.now().minusDays(60),
                LocalDateTime.now().minusDays(1),
                BigDecimal.ZERO);
        membershipRepository.save(expired);
        UserMembership membership = subscriptionService.getMembershipStatus(7L);
        assertEquals(MembershipStatus.EXPIRED, membership.getStatus(), "Lazy expire should set EXPIRED");

        assertThrows(MembershipNotFoundException.class,
                () -> subscriptionService.upgradeTier(7L, goldTierId),
                "Expired membership upgrade should be rejected (no active membership)");
    }

    static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected=" + expected + " actual=" + actual);
        }
    }

    static void assertEquals(BigDecimal expected, BigDecimal actual, String message) {
        if (expected.compareTo(actual) != 0) {
            throw new AssertionError(message + ": expected=" + expected + " actual=" + actual);
        }
    }

    static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + " actual=" + actual);
        }
    }

    static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + " actual=" + actual);
        }
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    static void assertThrows(Class<? extends Exception> expected, Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message + ": expected " + expected.getSimpleName() + " but no exception thrown");
        } catch (Exception e) {
            if (!expected.isInstance(e)) {
                throw new AssertionError(message + ": expected " + expected.getSimpleName()
                        + " but got " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
            }
        }
    }
}
