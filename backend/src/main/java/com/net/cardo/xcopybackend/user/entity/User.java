package com.net.cardo.xcopybackend.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true, length = 30)
    private String username;

    @Column(length = 50)
    private String displayName;

    @Column(length = 512)
    private String avatarUrl;

    private String password;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int tokenVersion;
}
