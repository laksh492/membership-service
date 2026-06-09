package com.firstclub.membership.service;

import com.firstclub.membership.model.UserProfile;
import com.firstclub.membership.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class UserProfileService {

    private final UserProfileRepository profileRepository;

    public UserProfileService(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public UserProfile getOrCreate(Long userId) {
        return profileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile(userId);
                    profileRepository.save(profile);
                    log.info("Created user profile userId={}", userId);
                    return profile;
                });
    }

    public void updateOrderStats(Long userId, int totalOrderCount, BigDecimal monthlyOrderValue) {
        UserProfile profile = getOrCreate(userId);
        profile.setTotalOrderCount(totalOrderCount);
        profile.setMonthlyOrderValue(monthlyOrderValue != null ? monthlyOrderValue : BigDecimal.ZERO);
        log.info("Updated order stats userId={} totalOrderCount={} monthlyOrderValue={}",
                userId, totalOrderCount, profile.getMonthlyOrderValue());
    }

    public void setCohort(Long userId, String cohort) {
        UserProfile profile = getOrCreate(userId);
        profile.setCohort(cohort);
        log.info("Set cohort userId={} cohort={}", userId, cohort);
    }
}
