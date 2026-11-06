package com.net.cardo.xcopybackend.post.service;

import com.mongodb.client.MongoClient;
import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.repository.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PostCleanupIntegrationTests {
    @Autowired MongoClient client;
    @TempDir Path directory;
    MongoTemplate mongo;MediaStorageService media;FeedCacheService cache;
    final Instant now=Instant.parse("2026-11-06T12:00:00Z");
    final String owner=UUID.randomUUID().toString();String database;
    @BeforeEach void setup(){database="xcopy_delete_test_"+UUID.randomUUID().toString().replace("-","");mongo=new MongoTemplate(client,database);media=spy(new MediaStorageService(directory.toString()));cache=mock(FeedCacheService.class);}
    @AfterEach void cleanup(){if(database!=null && database.startsWith("xcopy_delete_test_"))mongo.getDb().drop();}
    Post post(boolean file) {
        var post=new Post();post.setUserId(owner);post.setCreatedAt(LocalDateTime.ofInstant(now,ZoneOffset.UTC));post.setContent("Delete me");
        if(file)post.setMediaUrl(media.store(new MockMultipartFile("file","x.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10,0,1,2,3}),UUID.fromString(owner)).url());
        return mongo.insert(post);
    }
    void child(String collection,String postId){mongo.getCollection(collection).insertOne(new Document("_id",new ObjectId()).append("postId",postId).append("userId","viewer"));}
    void mark(Post post){new PostDeletionRepositoryImpl(mongo).requestDeletion(post.getId(),owner,now);}
    PostCleanupService worker(Instant time){var w=new PostCleanupService(mongo,media,cache,Clock.fixed(time,ZoneOffset.UTC));w.afterPropertiesSet();return w;}
    Document raw(Post post){return mongo.getCollection("posts").find(new Document("_id",new ObjectId(post.getId()))).first();}
    long children(String collection,Post post){return mongo.getCollection(collection).countDocuments(new Document("postId",post.getId()));}

    @Test void intentIsAtomicHiddenAndRepeatedDeletePreservesRetryState() {
        var post=post(false);mark(post);var first=raw(post);
        assertTrue(first.getBoolean("deletionRequested"));assertNotNull(first.get("cleanupNextAttemptAt"));
        assertEquals(List.of(),new PostWindowRepositoryImpl(mongo).findWindow(owner,null,null,21));
        mongo.getCollection("posts").updateOne(new Document("_id",first.get("_id")),new Document("$set",new Document("cleanupAttempts",3).append("cleanupNextAttemptAt",Date.from(now.plusSeconds(60)))));
        new PostDeletionRepositoryImpl(mongo).requestDeletion(post.getId(),owner,now.plusSeconds(5));
        assertEquals(3,raw(post).getInteger("cleanupAttempts"));assertEquals(Date.from(now.plusSeconds(60)),raw(post).getDate("cleanupNextAttemptAt"));
    }
    @Test void mediaFailureRetainsIntentAndANewWorkerFinishesAfterRestart() throws Exception {
        var post=post(true);child("likes",post.getId());child("comments",post.getId());mark(post);
        doThrow(new IOException("disk unavailable")).doCallRealMethod().when(media).deleteOrThrow(post.getMediaUrl());
        worker(now).runBatch();
        assertNotNull(raw(post));assertEquals(1,raw(post).getInteger("cleanupAttempts"));assertNull(raw(post).get("cleanupLeaseToken"));
        assertEquals(0,children("likes",post));assertEquals(0,children("comments",post));
        assertTrue(Files.exists(directory.resolve(post.getMediaUrl().substring("/media/posts/".length()))));
        worker(now.plusSeconds(6)).runBatch();assertNull(raw(post));
        assertFalse(Files.exists(directory.resolve(post.getMediaUrl().substring("/media/posts/".length()))));
        assertEquals("0 0",Files.readString(directory.resolve(".quota-v1/total")).trim());
    }
    @Test void cacheFailureAfterFileRemovalDoesNotLoseTheJobOrReleaseQuotaTwice() throws Exception {
        var post=post(true);mark(post);
        doThrow(new IllegalStateException("Redis down")).doNothing().when(cache).invalidate(owner);
        worker(now).runBatch();assertNotNull(raw(post));
        assertEquals("0 0",Files.readString(directory.resolve(".quota-v1/total")).trim());
        worker(now.plusSeconds(6)).runBatch();assertNull(raw(post));
        assertEquals("0 0",Files.readString(directory.resolve(".quota-v1/total")).trim());
    }
    @Test void expiredLeaseIsReclaimedAndStaleOwnerCannotRemoveTheNewClaim() {
        var post=post(false);mark(post);var firstWorker=worker(now);var first=firstWorker.claimNext();
        assertNotNull(first);assertNull(worker(now).claimNext());
        var secondWorker=worker(now.plusSeconds(31));var second=secondWorker.claimNext();assertNotNull(second);
        assertNotEquals(first.get("cleanupLeaseToken"),second.get("cleanupLeaseToken"));
        firstWorker.finish(first);assertNotNull(raw(post));assertEquals(second.get("cleanupLeaseToken"),raw(post).get("cleanupLeaseToken"));
        secondWorker.finish(second);assertNull(raw(post));
    }
    @Test void oneFailingJobDoesNotBlockOtherPosts() throws Exception {
        var bad=post(false);bad.setMediaUrl("/media/posts/failing.png");mongo.save(bad);mark(bad);
        var good=post(false);mark(good);
        doThrow(new IOException("persistent disk error")).when(media).deleteOrThrow(bad.getMediaUrl());
        worker(now).runBatch();assertNotNull(raw(bad));assertNull(raw(good));
        assertTrue(raw(bad).getDate("cleanupNextAttemptAt").after(Date.from(now)));
    }
    @Test void lateChildWritesAfterPhysicalDeletionAreEventuallyRemovedButLiveParentsSurvive() {
        var deleted=post(false);var active=post(false);mark(deleted);worker(now).runBatch();assertNull(raw(deleted));
        child("likes",deleted.getId());child("comments",deleted.getId());child("likes",active.getId());child("comments",active.getId());
        worker(now).runBatch();assertEquals(0,children("likes",deleted));assertEquals(0,children("comments",deleted));
        assertEquals(1,children("likes",active));assertEquals(1,children("comments",active));
    }
    @Test void boundedRepairTraversesMixedIdsAndPreservesLegacyStringHexParents() {
        String legacyParent="0123456789abcdef01234567";
        mongo.getCollection("posts").insertOne(new Document("_id",legacyParent).append("userId",owner));
        var likes=mongo.getCollection("likes");
        likes.insertMany(List.of(new Document("_id","a").append("postId","missing"),new Document("_id","z").append("postId",legacyParent),
                new Document("_id",new ObjectId()).append("postId","missing"),new Document("_id",new ObjectId()).append("postId","missing"),
                new Document("_id",new ObjectId()).append("postId",legacyParent)));
        var w=worker(now);ReflectionTestUtils.setField(w,"orphanBatchSize",2);
        for(int i=0;i<6;i++)w.repairOrphans("likes");
        assertEquals(2,likes.countDocuments());assertEquals(2,likes.countDocuments(new Document("postId",legacyParent)));
    }
}
