package com.net.cardo.xcopybackend.passwordreset.entity;

import com.net.cardo.xcopybackend.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name="PASSWORDRESETTOKEN")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private UUID userId ;

    @Column
    private String token;

    @Column
    private LocalDateTime expiryDate;

    @Column
    private boolean used ;



}
