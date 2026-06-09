package com.firstclub.membership.dto;

import com.firstclub.membership.enums.MembershipStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MembershipStatusResult {
    private Long userId;
    private Long membershipId;
    private Long planId;
    private Long tierId;
    private MembershipStatus status;
    private LocalDateTime expiryDate;
    private long daysRemaining;
    private boolean active;

    @Override
    public String toString() {
        return "MembershipStatusResult{userId=" + userId + ", membershipId=" + membershipId
                + ", planId=" + planId + ", tierId=" + tierId + ", status=" + status
                + ", expiryDate=" + expiryDate + ", daysRemaining=" + daysRemaining
                + ", active=" + active + "}";
    }
}
