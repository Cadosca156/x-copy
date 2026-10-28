package com.net.cardo.xcopybackend.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "user_follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followed_id"}),
        indexes = @Index(name = "idx_follows_follower", columnList = "follower_id"))
@Getter
@Setter
public class UserFollow {
    @Id
    private String id;
    @Column(name = "follower_id", nullable = false)
    private UUID followerId;
    @Column(name = "followed_id", nullable = false)
    private UUID followedId;
}
