package com.firstclub.membership.repository;

import com.firstclub.membership.model.MembershipPlan;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanRepository {
    MembershipPlan save(MembershipPlan plan);

    Optional<MembershipPlan> findById(Long id);

    List<MembershipPlan> findAll();
}
