package com.firstclub.membership.dto;

import com.firstclub.membership.enums.MembershipStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class SubscribeResult {
    private Long membershipId;
    private Long userId;
    private Long planId;
    private Long tierId;
    private BigDecimal priceCharged;
    private MembershipStatus status;
    private LocalDateTime expiryDate;

    @Override
    public String toString() {
        return "SubscribeResult{membershipId=" + membershipId + ", userId=" + userId
                + ", planId=" + planId + ", tierId=" + tierId + ", priceCharged=" + priceCharged
                + ", status=" + status + ", expiryDate=" + expiryDate + "}";
    }
}
