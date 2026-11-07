package com.net.cardo.xcopybackend.post.repository;

import com.mongodb.client.MongoClient;
import com.net.cardo.xcopybackend.config.MongoQueryIndexMigration;
import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.service.FeedCursor;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostWindowQueryIntegrationTests {
    @Autowired MongoClient client;
    MongoTemplate mongo;String database;
    @BeforeEach void seed() {
        database="xcopy_cursor_test_"+UUID.randomUUID().toString().replace("-","");mongo=new MongoTemplate(client,database);
        new MongoQueryIndexMigration(mongo).migrate();
        mongo.getCollection("posts").insertMany(IntStream.range(0,1000).mapToObj(i->new Document("_id",new ObjectId())
                .append("createdAt",new Date(i*1000L)).append("userId",i%5==0?"author-a":"author-b")).toList());
    }
    @AfterEach void cleanup(){if(database!=null && database.startsWith("xcopy_cursor_test_"))mongo.getDb().drop();}
    @Test void earlyAndDeepWindowsUseIndexesWithoutSkipOrCollectionSort() {
        for(long millis:new long[]{800000,300000}) {
            explain(null,millis,MongoQueryIndexMigration.FEED_INDEX);
            explain("author-a",millis,MongoQueryIndexMigration.AUTHOR_INDEX);
        }
    }
    void explain(String author,long millis,String expectedIndex) {
        var position=new FeedCursor.Position(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis),ZoneId.systemDefault()),"ffffffffffffffffffffffff");
        var query=PostWindowRepositoryImpl.query(author,null,position,21);
        var converter=mongo.getConverter();var entity=converter.getMappingContext().getPersistentEntity(Post.class);
        Document filter=new QueryMapper(converter).getMappedObject(query.getQueryObject(),entity);
        var command=new Document("find","posts").append("filter",filter).append("sort",new Document("createdAt",-1).append("_id",-1))
                .append("limit",21).append("maxTimeMS",3000);
        Document result=mongo.getDb().runCommand(new Document("explain",command).append("verbosity","executionStats"));
        String plan=result.get("queryPlanner",Document.class).get("winningPlan",Document.class).toJson();
        assertTrue(plan.contains(expectedIndex),plan);assertFalse(plan.contains("COLLSCAN"),plan);assertFalse(plan.contains("\"SORT\""),plan);
        var stats=result.get("executionStats",Document.class);assertEquals(21,((Number)stats.get("nReturned")).intValue());
        assertTrue(((Number)stats.get("totalDocsExamined")).intValue()<=30,stats.toJson());
        var rows=new PostWindowRepositoryImpl(mongo).findWindow(author,null,position,21);assertEquals(21,rows.size());
    }
}
