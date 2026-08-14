package com.spms.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spms.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
