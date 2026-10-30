package com.net.cardo.xcopybackend.post.repository;

import com.net.cardo.xcopybackend.post.entity.Post;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends MongoRepository<Post, String> {

    List<Post> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable page);

    List<Post> findByUserIdInOrderByCreatedAtDesc(List<String> userIds, org.springframework.data.domain.Pageable page);

    List<Post> findByUserIdOrderByCreatedAtDesc(String userId, org.springframework.data.domain.Pageable page);

}
