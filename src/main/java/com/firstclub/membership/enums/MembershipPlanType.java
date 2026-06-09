package com.firstclub.membership.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipPlanType {
    MONTHLY(30),
    QUARTERLY(90),
    YEARLY(365);

    private final int durationDays;
}
