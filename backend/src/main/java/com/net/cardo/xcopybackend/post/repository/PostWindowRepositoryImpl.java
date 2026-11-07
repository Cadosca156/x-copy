package com.net.cardo.xcopybackend.post.repository;

import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.service.FeedCursor;
import java.time.Duration;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

public class PostWindowRepositoryImpl implements PostWindowRepository {
    private final MongoTemplate mongo;
    public PostWindowRepositoryImpl(MongoTemplate mongo){this.mongo=mongo;}
    @Override public List<Post> findWindow(String author,List<String> authors,FeedCursor.Position before,int limit) {
        return mongo.find(query(author,authors,before,limit),Post.class);
    }
    static Query query(String author,List<String> authors,FeedCursor.Position before,int limit) {
        if(limit<1 || limit>51)throw new IllegalArgumentException("Invalid post window limit");
        var query=Query.query(Criteria.where("createdAt").ne(null));
        query.addCriteria(Criteria.where("deletionRequested").ne(true));
        if(author!=null)query.addCriteria(Criteria.where("userId").is(author));
        else if(authors!=null)query.addCriteria(Criteria.where("userId").in(authors));
        if(before!=null)query.addCriteria(new Criteria().orOperator(
                Criteria.where("createdAt").lt(before.createdAt()),
                new Criteria().andOperator(Criteria.where("createdAt").is(before.createdAt()),Criteria.where("id").lt(before.id()))));
        return query.with(Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")))
                .limit(limit).maxTime(Duration.ofSeconds(3));
    }
}
