package com.firstclub.membership.model;

import com.firstclub.membership.enums.MembershipPlanType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public class MembershipPlan {
    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    private Long id;
    private MembershipPlanType planType;
    private BigDecimal price;
    private boolean active;

    public MembershipPlan() {
        this.id = ID_COUNTER.incrementAndGet();
    }

    @Override
    public String toString() {
        return "MembershipPlan{id=" + id + ", planType=" + planType + ", price=" + price
                + ", active=" + active + "}";
    }
}
