package com.firstclub.membership.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UserProfile {
    private Long id;
    private String cohort;
    private int totalOrderCount;
    private BigDecimal monthlyOrderValue = BigDecimal.ZERO;

    public UserProfile(Long userId, String cohort) {
        this.id = userId;
        this.cohort = cohort;
        this.totalOrderCount = 0;
        this.monthlyOrderValue = BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return "UserProfile{id=" + id + ", cohort='" + cohort + "', totalOrderCount="
                + totalOrderCount + ", monthlyOrderValue=" + monthlyOrderValue + "}";
    }
}
