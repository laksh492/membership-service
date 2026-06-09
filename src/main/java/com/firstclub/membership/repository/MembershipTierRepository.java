package com.firstclub.membership.repository;

import com.firstclub.membership.enums.TierType;
import com.firstclub.membership.model.MembershipTier;

import java.util.List;
import java.util.Optional;

public interface MembershipTierRepository {
    MembershipTier save(MembershipTier tier);

    Optional<MembershipTier> findById(Long id);

    List<MembershipTier> findAll();

    List<MembershipTier> findAllOrderedByRank();

    Optional<MembershipTier> findByTierType(TierType tierType);
}
