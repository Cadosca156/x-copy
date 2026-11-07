package com.net.cardo.xcopybackend.post.repository;

import com.net.cardo.xcopybackend.post.entity.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends MongoRepository<Post, String>, PostWindowRepository, PostDeletionRepository {

    @Query("{ 'deletionRequested': { '$ne': true } }")
    List<Post> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable page);

    @Query("{ 'userId': { '$in': ?0 }, 'deletionRequested': { '$ne': true } }")
    List<Post> findByUserIdInOrderByCreatedAtDesc(List<String> userIds, org.springframework.data.domain.Pageable page);

    @Query("{ 'userId': ?0, 'deletionRequested': { '$ne': true } }")
    List<Post> findByUserIdOrderByCreatedAtDesc(String userId, org.springframework.data.domain.Pageable page);

    @Query(value="{ '_id': ?0, 'deletionRequested': { '$ne': true } }",exists=true)
    boolean existsActiveById(String id);

}
