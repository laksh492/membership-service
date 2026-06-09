package com.firstclub.membership.model;

import com.firstclub.membership.enums.BenefitType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public class Benefit {
    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    private Long id;
    private Long tierId;
    private BenefitType benefitType;
    private BigDecimal value;
    private String description;

    public Benefit() {
        this.id = ID_COUNTER.incrementAndGet();
    }

    @Override
    public String toString() {
        return "Benefit{id=" + id + ", tierId=" + tierId + ", benefitType=" + benefitType
                + ", value=" + value + ", description='" + description + "'}";
    }
}
