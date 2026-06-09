package com.firstclub.membership.repository;

import com.firstclub.membership.model.Benefit;

import java.util.List;
import java.util.Optional;

public interface BenefitRepository {
    Benefit save(Benefit benefit);

    Optional<Benefit> findById(Long id);

    List<Benefit> findAll();

    List<Benefit> findByTierId(Long tierId);
}
