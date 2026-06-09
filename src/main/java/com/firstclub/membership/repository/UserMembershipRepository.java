package com.firstclub.membership.repository;

import com.firstclub.membership.model.UserMembership;

import java.util.List;
import java.util.Optional;

public interface UserMembershipRepository {
    UserMembership save(UserMembership membership);

    Optional<UserMembership> findById(Long id);

    List<UserMembership> findAll();

    Optional<UserMembership> findActiveByUserId(Long userId);
}
