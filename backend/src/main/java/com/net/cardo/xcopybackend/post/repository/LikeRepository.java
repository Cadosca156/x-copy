package com.net.cardo.xcopybackend.post.repository;

import com.net.cardo.xcopybackend.post.entity.Like;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import com.net.cardo.xcopybackend.post.repository.projection.PostCount;
import com.net.cardo.xcopybackend.post.repository.projection.LikedPostId;
import java.util.List;

public interface LikeRepository extends MongoRepository<Like, String>, LikeWriteRepository {

    boolean existsByPostIdAndUserId(
            String postId,
            String userId
    );

    void deleteByPostIdAndUserId(
            String postId,
            String userId
    );

    long countByPostId(String postId);

    @Aggregation(pipeline = {
            "{ '$match': { 'postId': { '$in': ?0 } } }",
            "{ '$group': { '_id': '$postId', 'count': { '$sum': 1 } } }",
            "{ '$project': { '_id': 0, 'postId': '$_id', 'count': 1 } }"
    })
    List<PostCount> countForPosts(List<String> postIds);

    @Aggregation(pipeline = {
            "{ '$match': { 'postId': { '$in': ?0 }, 'userId': ?1 } }",
            "{ '$group': { '_id': '$postId' } }",
            "{ '$project': { '_id': 0, 'postId': '$_id' } }"
    })
    List<LikedPostId> findLikedPostIds(List<String> postIds, String viewerId);
}
