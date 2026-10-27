package com.net.cardo.xcopybackend.user.repository;

import com.net.cardo.xcopybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID userId);
    boolean existsByUsernameIgnoreCase(String username);
}
