package com.firstclub.membership.repository;

import com.firstclub.membership.enums.TierType;
import com.firstclub.membership.exception.DuplicateEntityException;
import com.firstclub.membership.model.MembershipTier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryMembershipTierRepository implements MembershipTierRepository {
    private final Map<Long, MembershipTier> store = new HashMap<>();

    @Override
    public MembershipTier save(MembershipTier tier) {
        if (store.containsKey(tier.getId())) {
            throw new DuplicateEntityException("MembershipTier", tier.getId());
        }
        store.put(tier.getId(), tier);
        return tier;
    }

    @Override
    public Optional<MembershipTier> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<MembershipTier> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<MembershipTier> findAllOrderedByRank() {
        return store.values().stream()
                .sorted(Comparator.comparingInt(t -> t.getTierType().getRank()))
                .toList();
    }

    @Override
    public Optional<MembershipTier> findByTierType(TierType tierType) {
        return store.values().stream()
                .filter(t -> t.getTierType() == tierType)
                .findFirst();
    }
}
