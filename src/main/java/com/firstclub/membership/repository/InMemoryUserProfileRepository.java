package com.firstclub.membership.repository;

import com.firstclub.membership.exception.DuplicateEntityException;
import com.firstclub.membership.model.UserProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserProfileRepository implements UserProfileRepository {
    private final Map<Long, UserProfile> store = new HashMap<>();

    @Override
    public UserProfile save(UserProfile profile) {
        if (store.containsKey(profile.getId())) {
            throw new DuplicateEntityException("UserProfile", profile.getId());
        }
        store.put(profile.getId(), profile);
        return profile;
    }

    @Override
    public Optional<UserProfile> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<UserProfile> findAll() {
        return new ArrayList<>(store.values());
    }
}
