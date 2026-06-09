package com.firstclub.membership.model;

import com.firstclub.membership.enums.MembershipStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public class UserMembership {
    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    private Long id;
    private Long userId;
    private Long planId;
    private Long currentTierId;
    private MembershipStatus status;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private LocalDateTime cancelledAt;

    public UserMembership() {
        this.id = ID_COUNTER.incrementAndGet();
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiryDate != null && now.isAfter(expiryDate);
    }

    @Override
    public String toString() {
        return "UserMembership{id=" + id + ", userId=" + userId + ", planId=" + planId
                + ", currentTierId=" + currentTierId + ", status=" + status + ", startDate="
                + startDate + ", expiryDate=" + expiryDate + ", cancelledAt=" + cancelledAt + "}";
    }
}
