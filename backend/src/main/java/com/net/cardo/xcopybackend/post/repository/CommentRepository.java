package com.net.cardo.xcopybackend.post.repository;

import com.net.cardo.xcopybackend.post.entity.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import com.net.cardo.xcopybackend.post.repository.projection.PostCount;

import java.util.List;

public interface CommentRepository
        extends MongoRepository<Comment, String> {

    List<Comment> findByPostIdOrderByCreatedAtDesc(String postId, org.springframework.data.domain.Pageable page);

    long countByPostId(String postId);

    @Aggregation(pipeline = {
            "{ '$match': { 'postId': { '$in': ?0 } } }",
            "{ '$group': { '_id': '$postId', 'count': { '$sum': 1 } } }",
            "{ '$project': { '_id': 0, 'postId': '$_id', 'count': 1 } }"
    })
    List<PostCount> countForPosts(List<String> postIds);
}
