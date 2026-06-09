package com.firstclub.membership.repository.impl;

import com.firstclub.membership.exception.DuplicateEntityException;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.repository.MembershipPlanRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryMembershipPlanRepository implements MembershipPlanRepository {
    private final Map<Long, MembershipPlan> store = new HashMap<>();

    @Override
    public MembershipPlan save(MembershipPlan plan) {
        if (store.containsKey(plan.getId())) {
            throw new DuplicateEntityException("MembershipPlan", plan.getId());
        }
        store.put(plan.getId(), plan);
        return plan;
    }

    @Override
    public Optional<MembershipPlan> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<MembershipPlan> findAll() {
        return new ArrayList<>(store.values());
    }
}
