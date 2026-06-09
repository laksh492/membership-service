package com.firstclub.membership.model;

import com.firstclub.membership.enums.MembershipStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private BigDecimal amountPaid = BigDecimal.ZERO;
    private LocalDateTime createdAt;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private LocalDateTime cancelledAt;

    public UserMembership(Long userId, Long planId, Long currentTierId,
                          LocalDateTime startDate, LocalDateTime expiryDate, BigDecimal amountPaid) {
        this.id = ID_COUNTER.incrementAndGet();
        this.userId = userId;
        this.planId = planId;
        this.currentTierId = currentTierId;
        this.status = MembershipStatus.ACTIVE;
        this.amountPaid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.startDate = startDate;
        this.expiryDate = expiryDate;
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiryDate != null && now.isAfter(expiryDate);
    }

    public long daysRemaining(LocalDateTime now) {
        if (expiryDate == null || status != MembershipStatus.ACTIVE) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), expiryDate.toLocalDate()));
    }

    @Override
    public String toString() {
        return "UserMembership{id=" + id + ", userId=" + userId + ", planId=" + planId
                + ", currentTierId=" + currentTierId + ", status=" + status + ", amountPaid="
                + amountPaid + ", createdAt=" + createdAt + ", startDate=" + startDate
                + ", expiryDate=" + expiryDate + ", cancelledAt=" + cancelledAt + "}";
    }
}
