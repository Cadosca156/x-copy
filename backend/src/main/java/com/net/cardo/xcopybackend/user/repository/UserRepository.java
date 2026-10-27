package com.net.cardo.xcopybackend.user.repository;

import com.net.cardo.xcopybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID userId);
    boolean existsByUsernameIgnoreCase(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.password = :password where u.email = :email "
            + "and (u.password is null or trim(u.password) = '')")
    int setInitialPassword(@Param("email") String email, @Param("password") String encodedPassword);
}
