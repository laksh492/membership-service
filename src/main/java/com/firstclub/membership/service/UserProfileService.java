package com.firstclub.membership.service;

import com.firstclub.membership.exception.DuplicateEntityException;
import com.firstclub.membership.exception.UserProfileNotFoundException;
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

    public UserProfile createUser(Long userId, String cohort) {
        profileRepository.findById(userId).ifPresent(existing -> {
            log.warn("Create user rejected userId={} reason={}",
                    userId, DuplicateEntityException.class.getSimpleName());
            throw new DuplicateEntityException("UserProfile", userId);
        });

        UserProfile profile = new UserProfile(userId, cohort);
        profileRepository.save(profile);
        log.info("Created user profile userId={} cohort={}", userId, cohort);
        return profile;
    }

    public UserProfile getUser(Long userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User profile not found userId={} reason={}",
                            userId, UserProfileNotFoundException.class.getSimpleName());
                    return new UserProfileNotFoundException(userId);
                });
    }

    public void updateOrderStats(Long userId, int totalOrderCount, BigDecimal monthlyOrderValue) {
        UserProfile profile = getUser(userId);
        profile.setTotalOrderCount(totalOrderCount);
        profile.setMonthlyOrderValue(monthlyOrderValue != null ? monthlyOrderValue : BigDecimal.ZERO);
        log.info("Updated order stats userId={} totalOrderCount={} monthlyOrderValue={}",
                userId, totalOrderCount, profile.getMonthlyOrderValue());
    }

    public void setCohort(Long userId, String cohort) {
        UserProfile profile = getUser(userId);
        profile.setCohort(cohort);
        log.info("Set cohort userId={} cohort={}", userId, cohort);
    }
}
