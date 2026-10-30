package com.net.cardo.xcopybackend.passwordreset.repository;

import com.net.cardo.xcopybackend.passwordreset.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByToken(String token);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update PasswordResetToken t set t.used = true where t.userId = :userId")
    int invalidateForUser(@org.springframework.data.repository.query.Param("userId") UUID userId);
}
