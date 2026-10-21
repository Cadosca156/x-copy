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
public class Post {

    @Id
    private String id;

    private String userId;

    private String content;

    private String mediaUrl;

    private String mediaType;

    private LocalDateTime createdAt;

    private int likes;

    private int reposts;

    private int comments;

}