package com.net.cardo.xcopybackend.post.repository;
import java.time.LocalDateTime;
public interface LikeWriteRepository {
    void ensureLiked(String postId,String userId,LocalDateTime createdAt);
}
