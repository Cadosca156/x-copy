package com.net.cardo.xcopybackend.post.repository;

import com.net.cardo.xcopybackend.post.entity.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository
        extends MongoRepository<Comment, String> {

    List<Comment> findByPostIdOrderByCreatedAtDesc(String postId, org.springframework.data.domain.Pageable page);

    long countByPostId(String postId);
}
