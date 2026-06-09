package com.firstclub.membership.repository;

import com.firstclub.membership.model.UserProfile;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository {
    UserProfile save(UserProfile profile);

    Optional<UserProfile> findById(Long id);

    List<UserProfile> findAll();
}
