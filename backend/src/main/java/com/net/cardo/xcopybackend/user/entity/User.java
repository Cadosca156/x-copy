package com.net.cardo.xcopybackend.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "users_email_key_unique", columnNames = "email_key"))
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_key", nullable = false, length = 512)
    private String emailKey;

    @PrePersist @PreUpdate
    private void synchronizeEmailIdentity() {
        email = com.net.cardo.xcopybackend.auth.EmailAddress.clean(email);
        emailKey = com.net.cardo.xcopybackend.auth.EmailAddress.key(email);
    }

    @Column(unique = true, length = 30)
    private String username;

    @Column(length = 50)
    private String displayName;

    @Column(length = 512)
    private String avatarUrl;

    private String password;

    @Column(name = "token_version", nullable = false, columnDefinition = "integer default 0")
    private int credentialVersion;

    // Prevent stale profile/avatar entities from restoring credentials changed by another request.
    @Version
    @Column(name = "row_version", nullable = false, columnDefinition = "bigint default 0")
    private long rowVersion;
}
