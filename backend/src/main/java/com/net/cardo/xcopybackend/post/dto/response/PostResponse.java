package com.net.cardo.xcopybackend.post.dto.response;

import java.time.LocalDateTime;
import java.io.Serializable;

public record PostResponse(
        String id,
        String userId,
        String username,
        String displayName,
        String content,
        String mediaType,
        String mediaUrl,
        LocalDateTime createdAt,
        long likesCount,
        long commentsCount,
        boolean likedByMe,
        String avatarUrl
) implements Serializable {}
