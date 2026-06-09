package com.firstclub.membership.repository.impl;

import com.firstclub.membership.exception.DuplicateEntityException;
import com.firstclub.membership.model.Benefit;
import com.firstclub.membership.repository.BenefitRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryBenefitRepository implements BenefitRepository {
    private final Map<Long, Benefit> store = new HashMap<>();

    @Override
    public Benefit save(Benefit benefit) {
        if (store.containsKey(benefit.getId())) {
            throw new DuplicateEntityException("Benefit", benefit.getId());
        }
        store.put(benefit.getId(), benefit);
        return benefit;
    }

    @Override
    public Optional<Benefit> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Benefit> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Benefit> findByTierId(Long tierId) {
        return store.values().stream()
                .filter(b -> tierId.equals(b.getTierId()))
                .toList();
    }
}
