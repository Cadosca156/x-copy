package com.net.cardo.xcopybackend.post.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Document(collection = "posts")
public class Post implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private String userId;

    private String content;

    private String mediaUrl;

    private String mediaType;

    private LocalDateTime createdAt;

    private boolean deletionRequested;
    private java.time.Instant deletionRequestedAt;
    private java.time.Instant cleanupNextAttemptAt;
    private int cleanupAttempts;
    private String cleanupLeaseToken;
    private java.time.Instant cleanupLeaseUntil;

    private int likes;

    private int reposts;

    private int comments;

}
