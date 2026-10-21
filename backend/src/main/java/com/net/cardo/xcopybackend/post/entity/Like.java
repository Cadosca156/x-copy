package com.net.cardo.xcopybackend.post.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "likes")
public class Like {

    @Id
    private String id;

    private String postId;
    private String userId;

    private LocalDateTime createdAt;
}
