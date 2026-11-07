package com.net.cardo.xcopybackend.post.repository;

import com.mongodb.WriteConcern;
import com.net.cardo.xcopybackend.post.entity.Post;
import java.time.Instant;
import java.util.Date;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.query.*;

public class PostDeletionRepositoryImpl implements PostDeletionRepository {
    private final MongoTemplate mongo;
    public PostDeletionRepositoryImpl(MongoTemplate mongo){this.mongo=mongo;}
    @Override public void requestDeletion(String id,String owner,Instant now) {
        var query=Query.query(Criteria.where("id").is(id).and("userId").is(owner).and("deletionRequested").ne(true));
        var entity=mongo.getConverter().getMappingContext().getPersistentEntity(Post.class);
        var filter=new QueryMapper(mongo.getConverter()).getMappedObject(query.getQueryObject(),entity);
        // Intent and queue metadata are one journaled document update, not two writes.
        var fields=new Document("deletionRequested",true).append("deletionRequestedAt",Date.from(now))
                .append("cleanupNextAttemptAt",Date.from(now)).append("cleanupAttempts",0);
        mongo.getCollection("posts").withWriteConcern(WriteConcern.JOURNALED)
                .updateOne(filter,new Document("$set",fields));
    }
}
