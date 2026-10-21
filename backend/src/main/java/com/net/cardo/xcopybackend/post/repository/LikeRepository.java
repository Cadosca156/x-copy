package com.net.cardo.xcopybackend.post.repository;

import com.net.cardo.xcopybackend.post.entity.Like;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LikeRepository extends MongoRepository<Like, String> {

    boolean existsByPostIdAndUserId(
            String postId,
            String userId
    );

    void deleteByPostIdAndUserId(
            String postId,
            String userId
    );

    long countByPostId(String postId);
}
