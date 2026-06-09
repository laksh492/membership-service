package com.firstclub.membership.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpgradeResult {
    private Long membershipId;
    private Long userId;
    private Long previousTierId;
    private Long newTierId;
    private BigDecimal amountCharged;
    private boolean criteriaMet;
    private boolean freeUpgrade;

    @Override
    public String toString() {
        return "UpgradeResult{membershipId=" + membershipId + ", userId=" + userId
                + ", previousTierId=" + previousTierId + ", newTierId=" + newTierId
                + ", amountCharged=" + amountCharged + ", criteriaMet=" + criteriaMet
                + ", freeUpgrade=" + freeUpgrade + "}";
    }
}
