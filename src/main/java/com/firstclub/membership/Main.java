package com.firstclub.membership;

import com.firstclub.membership.dto.MembershipStatusResult;
import com.firstclub.membership.dto.SubscribeResult;
import com.firstclub.membership.dto.UpgradeResult;
import com.firstclub.membership.enums.BenefitType;
import com.firstclub.membership.enums.MatchPolicy;
import com.firstclub.membership.enums.MembershipPlanType;
import com.firstclub.membership.enums.MembershipStatus;
import com.firstclub.membership.enums.TierType;
import com.firstclub.membership.exception.ActiveMembershipExistsException;
import com.firstclub.membership.exception.AlreadyOnTierException;
import com.firstclub.membership.exception.InactivePlanException;
import com.firstclub.membership.exception.InvalidTierChangeException;
import com.firstclub.membership.exception.MembershipCancelledException;
import com.firstclub.membership.exception.MembershipExpiredException;
import com.firstclub.membership.exception.TierCriteriaNotMetException;
import com.firstclub.membership.model.Benefit;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.model.TierUpgradeCriteria;
import com.firstclub.membership.model.UserMembership;
import com.firstclub.membership.model.UserProfile;
import com.firstclub.membership.repository.BenefitRepository;
import com.firstclub.membership.repository.InMemoryBenefitRepository;
import com.firstclub.membership.repository.InMemoryMembershipPlanRepository;
import com.firstclub.membership.repository.InMemoryMembershipTierRepository;
import com.firstclub.membership.repository.InMemoryUserMembershipRepository;
import com.firstclub.membership.repository.InMemoryUserProfileRepository;
import com.firstclub.membership.repository.MembershipPlanRepository;
import com.firstclub.membership.repository.MembershipTierRepository;
import com.firstclub.membership.repository.UserMembershipRepository;
import com.firstclub.membership.repository.UserProfileRepository;
import com.firstclub.membership.service.MembershipCatalogService;
import com.firstclub.membership.service.MembershipSubscriptionService;
import com.firstclub.membership.service.MembershipTierChangeService;
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
    static BenefitRepository benefitRepository;
    static UserMembershipRepository membershipRepository;
    static UserProfileRepository profileRepository;

    static PricingService pricingService;
    static MembershipCatalogService catalogService;
    static UserProfileService profileService;
    static MembershipSubscriptionService subscriptionService;
    static MembershipTierChangeService tierChangeService;

    static Long monthlyPlanId;
    static Long inactivePlanId;
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
        benefitRepository = new InMemoryBenefitRepository();
        membershipRepository = new InMemoryUserMembershipRepository();
        profileRepository = new InMemoryUserProfileRepository();

        pricingService = new PricingService();
        catalogService = new MembershipCatalogService(planRepository, tierRepository, benefitRepository);
        profileService = new UserProfileService(profileRepository);
        subscriptionService = new MembershipSubscriptionService(
                membershipRepository, catalogService, pricingService);
        tierChangeService = new MembershipTierChangeService(
                membershipRepository, catalogService, pricingService, profileService, new CriteriaEvaluator());

        seedPlans();
        seedTiersAndBenefits();
        seedUserProfiles();

        log.info("Data layer initialized plans={} tiers={} profiles={}",
                planRepository.findAll().size(), tierRepository.findAll().size(),
                profileRepository.findAll().size());
    }

    private static void seedPlans() {
        MembershipPlan monthly = new MembershipPlan();
        monthly.setPlanType(MembershipPlanType.MONTHLY);
        monthly.setPrice(new BigDecimal("299"));
        monthly.setActive(true);
        planRepository.save(monthly);
        monthlyPlanId = monthly.getId();

        MembershipPlan quarterly = new MembershipPlan();
        quarterly.setPlanType(MembershipPlanType.QUARTERLY);
        quarterly.setPrice(new BigDecimal("799"));
        quarterly.setActive(true);
        planRepository.save(quarterly);

        MembershipPlan yearly = new MembershipPlan();
        yearly.setPlanType(MembershipPlanType.YEARLY);
        yearly.setPrice(new BigDecimal("2499"));
        yearly.setActive(true);
        planRepository.save(yearly);

        MembershipPlan inactive = new MembershipPlan();
        inactive.setPlanType(MembershipPlanType.MONTHLY);
        inactive.setPrice(new BigDecimal("199"));
        inactive.setActive(false);
        planRepository.save(inactive);
        inactivePlanId = inactive.getId();
    }

    private static void seedTiersAndBenefits() {
        MembershipTier silver = new MembershipTier();
        silver.setTierType(TierType.SILVER);
        silver.setPurchasePremium(BigDecimal.ZERO);
        tierRepository.save(silver);
        silverTierId = silver.getId();

        Benefit silverDeals = new Benefit();
        silverDeals.setBenefitType(BenefitType.EXCLUSIVE_DEALS);
        silverDeals.setValue(BigDecimal.ZERO);
        silverDeals.setDescription("Access to member-only deals");
        benefitRepository.save(silverDeals);
        silver.addBenefit(silverDeals);

        MembershipTier gold = new MembershipTier();
        gold.setTierType(TierType.GOLD);
        gold.setPurchasePremium(new BigDecimal("100"));
        TierUpgradeCriteria goldCriteria = new TierUpgradeCriteria();
        goldCriteria.setPolicy(MatchPolicy.AT_LEAST);
        goldCriteria.setRequiredCount(2);
        goldCriteria.getCriteria().add(new MinOrderCountCriterion(5));
        goldCriteria.getCriteria().add(new MinMonthlySpendCriterion(new BigDecimal("5000")));
        goldCriteria.getCriteria().add(new CohortCriterion("VIP"));
        gold.setUpgradeCriteria(goldCriteria);
        tierRepository.save(gold);
        goldTierId = gold.getId();

        Benefit goldDiscount = new Benefit();
        goldDiscount.setBenefitType(BenefitType.EXTRA_DISCOUNT);
        goldDiscount.setValue(new BigDecimal("10"));
        goldDiscount.setDescription("10% extra discount on orders");
        benefitRepository.save(goldDiscount);
        gold.addBenefit(goldDiscount);

        MembershipTier platinum = new MembershipTier();
        platinum.setTierType(TierType.PLATINUM);
        platinum.setPurchasePremium(new BigDecimal("300"));
        TierUpgradeCriteria platinumCriteria = new TierUpgradeCriteria();
        platinumCriteria.setPolicy(MatchPolicy.ALL);
        platinumCriteria.getCriteria().add(new MinOrderCountCriterion(15));
        platinumCriteria.getCriteria().add(new MinMonthlySpendCriterion(new BigDecimal("15000")));
        platinum.setUpgradeCriteria(platinumCriteria);
        tierRepository.save(platinum);
        platinumTierId = platinum.getId();

        Benefit platinumDelivery = new Benefit();
        platinumDelivery.setBenefitType(BenefitType.FREE_DELIVERY);
        platinumDelivery.setValue(BigDecimal.ZERO);
        platinumDelivery.setDescription("Free delivery on all orders");
        benefitRepository.save(platinumDelivery);
        platinum.addBenefit(platinumDelivery);

        Benefit platinumSupport = new Benefit();
        platinumSupport.setBenefitType(BenefitType.PRIORITY_SUPPORT);
        platinumSupport.setValue(BigDecimal.ZERO);
        platinumSupport.setDescription("Priority customer support");
        benefitRepository.save(platinumSupport);
        platinum.addBenefit(platinumSupport);
    }

    private static void seedUserProfiles() {
        UserProfile user1 = new UserProfile(1L);
        profileRepository.save(user1);

        UserProfile user2 = new UserProfile(2L);
        user2.setCohort("VIP");
        user2.setTotalOrderCount(10);
        user2.setMonthlyOrderValue(new BigDecimal("8000"));
        profileRepository.save(user2);

        UserProfile user3 = new UserProfile(3L);
        profileRepository.save(user3);
    }

    static void runAllTests() {
        runTest("testGetActivePlansAndTiers", Main::testGetActivePlansAndTiers);
        runTest("testSubscribeMonthlySilver", Main::testSubscribeMonthlySilver);
        runTest("testSubscribeDuplicateRejected", Main::testSubscribeDuplicateRejected);
        runTest("testSubscribeInactivePlanRejected", Main::testSubscribeInactivePlanRejected);
        runTest("testGetMembershipStatus", Main::testGetMembershipStatus);
        runTest("testCancelMembership", Main::testCancelMembership);
        runTest("testUpgradeAfterCancelRejected", Main::testUpgradeAfterCancelRejected);
        runTest("testPaidUpgradeGold", Main::testPaidUpgradeGold);
        runTest("testFreeUpgradeGold", Main::testFreeUpgradeGold);
        runTest("testDowngradeToSilver", Main::testDowngradeToSilver);
        runTest("testUpgradeToSameTierRejected", Main::testUpgradeToSameTierRejected);
        runTest("testDowngradeToHigherTierRejected", Main::testDowngradeToHigherTierRejected);
        runTest("testEvaluateAndUpgradeFree", Main::testEvaluateAndUpgradeFree);
        runTest("testEvaluateAndUpgradeCriteriaNotMet", Main::testEvaluateAndUpgradeCriteriaNotMet);
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

    static void testGetActivePlansAndTiers() {
        List<MembershipPlan> plans = catalogService.getActivePlans();
        assertEquals(3, plans.size(), "Active plan count");

        List<MembershipTier> tiers = catalogService.getTiers();
        assertEquals(3, tiers.size(), "Tier count");
        assertEquals(TierType.SILVER, tiers.get(0).getTierType(), "First tier should be Silver");
        assertEquals(TierType.PLATINUM, tiers.get(2).getTierType(), "Last tier should be Platinum");
    }

    static void testSubscribeMonthlySilver() {
        SubscribeResult result = subscriptionService.subscribe(1L, monthlyPlanId, silverTierId);
        assertEquals(new BigDecimal("299"), result.getPriceCharged(), "Subscribe price");
        assertEquals(MembershipStatus.ACTIVE, result.getStatus(), "Membership status");
        assertEquals(silverTierId, result.getTierId(), "Subscribed tier");
        assertTrue(result.getExpiryDate() != null, "Expiry date should be set");
    }

    static void testSubscribeDuplicateRejected() {
        assertThrows(ActiveMembershipExistsException.class,
                () -> subscriptionService.subscribe(1L, monthlyPlanId, silverTierId),
                "Duplicate subscribe should be rejected");
    }

    static void testSubscribeInactivePlanRejected() {
        assertThrows(InactivePlanException.class,
                () -> subscriptionService.subscribe(99L, inactivePlanId, silverTierId),
                "Inactive plan subscribe should be rejected");
    }

    static void testGetMembershipStatus() {
        MembershipStatusResult status = subscriptionService.getMembershipStatus(1L);
        assertEquals(MembershipStatus.ACTIVE, status.getStatus(), "Status should be ACTIVE");
        assertEquals(monthlyPlanId, status.getPlanId(), "Plan id");
        assertEquals(silverTierId, status.getTierId(), "Tier id");
        assertTrue(status.getDaysRemaining() > 0, "Days remaining should be positive");
        assertTrue(status.isActive(), "Should be active");
    }

    static void testCancelMembership() {
        subscriptionService.cancel(1L);
        MembershipStatusResult status = subscriptionService.getMembershipStatus(1L);
        assertEquals(MembershipStatus.CANCELLED, status.getStatus(), "Status should be CANCELLED");
    }

    static void testUpgradeAfterCancelRejected() {
        assertThrows(MembershipCancelledException.class,
                () -> tierChangeService.upgradeTier(1L, goldTierId),
                "Upgrade after cancel should be rejected");
    }

    static void testPaidUpgradeGold() {
        subscriptionService.subscribe(3L, monthlyPlanId, silverTierId);
        UpgradeResult result = tierChangeService.upgradeTier(3L, goldTierId);
        assertEquals(new BigDecimal("100"), result.getAmountCharged(), "Paid upgrade charge");
        assertFalse(result.isCriteriaMet(), "Criteria should not be met");
        assertEquals(goldTierId, result.getNewTierId(), "New tier should be Gold");
    }

    static void testFreeUpgradeGold() {
        subscriptionService.subscribe(4L, monthlyPlanId, silverTierId);
        profileService.updateOrderStats(4L, 5, new BigDecimal("6000"));
        UpgradeResult result = tierChangeService.upgradeTier(4L, goldTierId);
        assertEquals(BigDecimal.ZERO, result.getAmountCharged(), "Free upgrade charge");
        assertTrue(result.isCriteriaMet(), "Criteria should be met");
        assertTrue(result.isFreeUpgrade(), "Should be free upgrade");
    }

    static void testDowngradeToSilver() {
        tierChangeService.downgradeTier(3L, silverTierId);
        MembershipStatusResult status = subscriptionService.getMembershipStatus(3L);
        assertEquals(silverTierId, status.getTierId(), "Tier should be Silver after downgrade");
    }

    static void testUpgradeToSameTierRejected() {
        assertThrows(AlreadyOnTierException.class,
                () -> tierChangeService.upgradeTier(3L, silverTierId),
                "Upgrade to same tier should be rejected");
    }

    static void testDowngradeToHigherTierRejected() {
        assertThrows(InvalidTierChangeException.class,
                () -> tierChangeService.downgradeTier(3L, goldTierId),
                "Downgrade to higher tier should be rejected");
    }

    static void testEvaluateAndUpgradeFree() {
        subscriptionService.subscribe(5L, monthlyPlanId, silverTierId);
        profileService.updateOrderStats(5L, 5, new BigDecimal("6000"));
        UpgradeResult result = tierChangeService.evaluateAndUpgrade(5L);
        assertEquals(goldTierId, result.getNewTierId(), "Should upgrade to Gold");
        assertEquals(BigDecimal.ZERO, result.getAmountCharged(), "Evaluate upgrade should be free");
        assertTrue(result.isFreeUpgrade(), "Should be free upgrade");
    }

    static void testEvaluateAndUpgradeCriteriaNotMet() {
        subscriptionService.subscribe(6L, monthlyPlanId, silverTierId);
        assertThrows(TierCriteriaNotMetException.class,
                () -> tierChangeService.evaluateAndUpgrade(6L),
                "Evaluate upgrade without criteria should be rejected");
    }

    static void testMembershipLazyExpire() {
        UserMembership expired = new UserMembership();
        expired.setUserId(7L);
        expired.setPlanId(monthlyPlanId);
        expired.setCurrentTierId(silverTierId);
        expired.setStatus(MembershipStatus.ACTIVE);
        expired.setStartDate(LocalDateTime.now().minusDays(60));
        expired.setExpiryDate(LocalDateTime.now().minusDays(1));
        membershipRepository.save(expired);

        MembershipStatusResult status = subscriptionService.getMembershipStatus(7L);
        assertEquals(MembershipStatus.EXPIRED, status.getStatus(), "Lazy expire should set EXPIRED");

        assertThrows(MembershipExpiredException.class,
                () -> tierChangeService.upgradeTier(7L, goldTierId),
                "Expired membership upgrade should be rejected");
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
