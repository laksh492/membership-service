package com.firstclub.membership.repository;

import com.firstclub.membership.enums.MembershipStatus;
import com.firstclub.membership.exception.DuplicateEntityException;
import com.firstclub.membership.model.UserMembership;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserMembershipRepository implements UserMembershipRepository {
    private final Map<Long, UserMembership> store = new HashMap<>();

    @Override
    public UserMembership save(UserMembership membership) {
        if (store.containsKey(membership.getId())) {
            throw new DuplicateEntityException("UserMembership", membership.getId());
        }
        store.put(membership.getId(), membership);
        return membership;
    }

    @Override
    public Optional<UserMembership> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<UserMembership> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<UserMembership> findActiveByUserId(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return store.values().stream()
                .filter(m -> userId.equals(m.getUserId()))
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .filter(m -> !m.isExpired(now))
                .findFirst();
    }
}
