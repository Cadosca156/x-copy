package com.net.cardo.xcopybackend.post.dto.response;
import java.time.LocalDateTime;

public record CommentResponse(String id, String postId, String userId, String username,
        String displayName, String avatarUrl, String content, LocalDateTime createdAt) {}
