package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LikeIdempotencyIntegrationTests {
    @Autowired LikeService service;@Autowired LikeRepository likes;@Autowired PostRepository posts;@Autowired MongoTemplate mongo;
    Post post;String viewer;
    @BeforeEach void seed(){post=new Post();post.setUserId(UUID.randomUUID().toString());post.setCreatedAt(LocalDateTime.now());post=posts.save(post);viewer=UUID.randomUUID().toString();}
    @AfterEach void cleanup(){if(post!=null){mongo.getCollection("likes").deleteMany(new Document("postId",post.getId()));posts.deleteById(post.getId());}}
    @Test void retriesPreserveOneDocumentAndItsOriginalCreationTime() {
        var first=service.setLiked(post.getId(),viewer,true);assertTrue(first.likedByMe());assertEquals(1,first.likesCount());
        var key=new Document("postId",post.getId()).append("userId",viewer);var original=mongo.getCollection("likes").find(key).first();
        var repeated=service.setLiked(post.getId(),viewer,true);assertTrue(repeated.likedByMe());assertEquals(1,repeated.likesCount());
        var after=mongo.getCollection("likes").find(key).first();assertEquals(original.get("_id"),after.get("_id"));assertEquals(original.get("createdAt"),after.get("createdAt"));
        assertFalse(service.setLiked(post.getId(),viewer,false).likedByMe());assertEquals(0,service.setLiked(post.getId(),viewer,false).likesCount());
        assertEquals(0,likes.countByPostId(post.getId()));
    }
    @Test void concurrentPutRequestsForTheSamePairCreateOnlyOneLike() throws Exception {
        var pool=Executors.newFixedThreadPool(8);var gate=new CountDownLatch(1);var pending=new ArrayList<Future<Boolean>>();
        try {
            for(int i=0;i<8;i++)pending.add(pool.submit(()->{assertTrue(gate.await(5,TimeUnit.SECONDS));return service.setLiked(post.getId(),viewer,true).likedByMe();}));
            gate.countDown();for(var result:pending)assertTrue(result.get(10,TimeUnit.SECONDS));
            assertEquals(1,likes.countByPostId(post.getId()));
        } finally {gate.countDown();pool.shutdownNow();}
    }
    @Test void putIsRejectedForTombstonedPostButDeleteRemainsSafe() {
        service.setLiked(post.getId(),viewer,true);posts.requestDeletion(post.getId(),post.getUserId(),java.time.Instant.now());
        assertEquals(404,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.setLiked(post.getId(),viewer,true)).getStatusCode().value());
        assertFalse(service.setLiked(post.getId(),viewer,false).likedByMe());assertEquals(0,likes.countByPostId(post.getId()));
    }
}
