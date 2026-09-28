package com.net.cardo.xcopybackend.repository;

import com.net.cardo.xcopybackend.entity.TwoFactorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
    public interface TwoFactorRepository
            extends JpaRepository<TwoFactorCode, UUID> {
    Optional<TwoFactorCode> findByUserIdAndCode(UUID userId, String code);
    }
