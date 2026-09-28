package com.net.cardo.xcopybackend.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name="TWOFACTORCODE")
public class TwoFactorCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private UUID userId;
    @Column
    private String code;

    @Column
    private LocalDateTime expiresAt;
}
