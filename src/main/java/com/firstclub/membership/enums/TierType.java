package com.firstclub.membership.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TierType {
    SILVER(1),
    GOLD(2),
    PLATINUM(3);

    private final int rank;
}
