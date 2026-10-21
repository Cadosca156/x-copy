package com.net.cardo.xcopybackend.twofactor.repository;

import com.net.cardo.xcopybackend.twofactor.entity.TwoFactorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
    public interface TwoFactorRepository
            extends JpaRepository<TwoFactorCode, UUID> {
    Optional<TwoFactorCode> findByUserIdAndCode(UUID userId, String code);
    Optional<TwoFactorCode> findCodeByUserId(UUID userId);

    }
