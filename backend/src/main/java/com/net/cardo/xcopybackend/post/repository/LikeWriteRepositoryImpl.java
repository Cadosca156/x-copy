package com.net.cardo.xcopybackend.post.repository;
import com.net.cardo.xcopybackend.post.entity.Like;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

public class LikeWriteRepositoryImpl implements LikeWriteRepository {
    private final MongoTemplate mongo;
    public LikeWriteRepositoryImpl(MongoTemplate mongo){this.mongo=mongo;}
    @Override public void ensureLiked(String postId,String userId,LocalDateTime createdAt) {
        var query=Query.query(Criteria.where("postId").is(postId).and("userId").is(userId));
        // A retry leaves the original document ID and createdAt unchanged.
        var update=new Update().setOnInsert("postId",postId).setOnInsert("userId",userId).setOnInsert("createdAt",createdAt);
        mongo.upsert(query,update,Like.class);
    }
}
