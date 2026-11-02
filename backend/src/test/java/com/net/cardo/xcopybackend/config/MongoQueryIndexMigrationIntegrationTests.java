package com.net.cardo.xcopybackend.config;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ValidationOptions;
import com.net.cardo.xcopybackend.post.service.FeedCacheService;
import java.util.*;
import java.util.stream.IntStream;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MongoQueryIndexMigrationIntegrationTests {
    @Autowired MongoClient client;
    private MongoTemplate mongo;
    private MongoQueryIndexMigration migration;
    private FeedCacheService cache;
    private String database;
    @BeforeEach void setup() {
        database = "xcopy_index_test_"+UUID.randomUUID().toString().replace("-","");
        mongo = new MongoTemplate(client,database);
        cache = mock(FeedCacheService.class);
        migration = new MongoQueryIndexMigration(mongo,cache);
    }
    @AfterEach void cleanup() {
        if(database!=null && database.startsWith("xcopy_index_test_")) mongo.getDb().drop();
    }
    Document like(String id,String post,String user) {
        return new Document("_id",id).append("postId",post).append("userId",user)
                .append("createdAt",new Date(123456789)).append("legacyExtra","preserve me");
    }
    Document index(String collection,String name) {
        return mongo.getCollection(collection).listIndexes().into(new ArrayList<>()).stream()
                .filter(i->name.equals(i.getString("name"))).findFirst().orElseThrow();
    }
    @Test void createsAllFourIndexesAndRepeatingMigrationIsSafe() {
        migration.migrate();
        assertEquals(new Document("createdAt",-1).append("_id",-1),index("posts",MongoQueryIndexMigration.FEED_INDEX).get("key"));
        assertEquals(new Document("userId",1).append("createdAt",-1).append("_id",-1),index("posts",MongoQueryIndexMigration.AUTHOR_INDEX).get("key"));
        assertEquals(new Document("postId",1).append("createdAt",-1).append("_id",-1),index("comments",MongoQueryIndexMigration.COMMENTS_INDEX).get("key"));
        assertTrue(index("likes",MongoQueryIndexMigration.LIKES_INDEX).getBoolean("unique"));
        migration.migrate();
        assertEquals(3,mongo.getCollection("posts").listIndexes().into(new ArrayList<>()).size());
        assertEquals(2,mongo.getCollection("likes").listIndexes().into(new ArrayList<>()).size());
        assertEquals(0,mongo.getCollection(MongoQueryIndexMigration.ARCHIVE).countDocuments());
    }
    @Test void duplicateDocumentsAreArchivedBeforeRemovalAndOtherPairsRemain() {
        var source = mongo.getCollection("likes");
        List<Document> originals = List.of(like("a","post","user"),like("b","post","user"),
                like("c","post","user"),like("d","post","other"),like("e","elsewhere","user"));
        source.insertMany(originals);
        migration.migrate();
        assertEquals(3,source.countDocuments());
        var archived = mongo.getCollection(MongoQueryIndexMigration.ARCHIVE).find().into(new ArrayList<>());
        assertEquals(2,archived.size());
        for(var backup:archived) {
            var original = originals.stream().filter(d->d.get("_id").equals(backup.get("_id"))).findFirst().orElseThrow();
            assertEquals(original,backup.get("document",Document.class));
            assertEquals(0,source.countDocuments(new Document("_id",backup.get("_id"))));
        }
        assertEquals(1,source.countDocuments(new Document("postId","post").append("userId","user")));
        var conflict = assertThrows(MongoWriteException.class,()->source.insertOne(like("new","post","user")));
        assertEquals(11000,conflict.getError().getCode());
        source.insertOne(like("valid","post","third-user"));
        migration.migrate();
        assertEquals(2,mongo.getCollection(MongoQueryIndexMigration.ARCHIVE).countDocuments());
        verify(cache,times(1)).invalidate();
    }
    @Test void preparedIndexRejectsNewDuplicatesAndInterruptedConversionCanResume() {
        var source = mongo.getCollection("likes");source.insertMany(List.of(like("a","post","user"),like("b","post","user")));
        source.createIndex(new Document("postId",1).append("userId",1),new IndexOptions().name("legacy_pair_index"));
        mongo.getDb().runCommand(new Document("collMod","likes").append("index",new Document("name","legacy_pair_index").append("prepareUnique",true)));
        assertThrows(MongoWriteException.class,()->source.insertOne(like("c","post","user")));
        migration.migrate();
        assertEquals(1,source.countDocuments());assertTrue(index("likes","legacy_pair_index").getBoolean("unique"));
    }
    @Test void archiveWriteFailureDeletesNothingAndRetryCompletes() {
        var source = mongo.getCollection("likes");source.insertMany(List.of(like("a","post","user"),like("b","post","user")));
        mongo.getDb().createCollection(MongoQueryIndexMigration.ARCHIVE,new CreateCollectionOptions().validationOptions(
                new ValidationOptions().validator(new Document("$jsonSchema",new Document("required",List.of("neverPresent"))))));
        assertThrows(RuntimeException.class,()->migration.migrate());
        assertEquals(2,source.countDocuments());
        assertEquals(0,mongo.getCollection(MongoQueryIndexMigration.ARCHIVE).countDocuments());
        verifyNoInteractions(cache);
        mongo.getDb().runCommand(new Document("collMod",MongoQueryIndexMigration.ARCHIVE).append("validationLevel","off"));
        migration.migrate();
        assertEquals(1,source.countDocuments());assertEquals(1,mongo.getCollection(MongoQueryIndexMigration.ARCHIVE).countDocuments());
    }
    @Test void malformedLegacyIdsStopMigrationWithoutDeletingThem() {
        mongo.getCollection("likes").insertOne(new Document("_id","bad").append("postId",List.of("array")).append("userId","user"));
        assertThrows(IllegalStateException.class,()->migration.migrate());
        assertEquals(1,mongo.getCollection("likes").countDocuments());
        assertEquals(0,mongo.getCollection(MongoQueryIndexMigration.ARCHIVE).countDocuments());
    }
    @Test void conflictingArchiveSnapshotDoesNotAllowDeletingTheActiveDocument() {
        var source = mongo.getCollection("likes");
        source.insertMany(List.of(like("a","post","user"),like("b","post","user")));
        mongo.getCollection(MongoQueryIndexMigration.ARCHIVE).insertMany(List.of(
                new Document("_id","a").append("document",new Document("incorrect",true)),
                new Document("_id","b").append("document",new Document("incorrect",true))));
        assertThrows(IllegalStateException.class,()->migration.migrate());
        assertEquals(2,source.countDocuments());
    }
    @Test void conflictingNamedIndexIsNotDroppedOrSilentlyReplaced() {
        mongo.getCollection("posts").createIndex(new Document("userId",1),
                new IndexOptions().name(MongoQueryIndexMigration.FEED_INDEX));
        assertThrows(IllegalStateException.class,()->migration.migrate());
        assertEquals(new Document("userId",1),index("posts",MongoQueryIndexMigration.FEED_INDEX).get("key"));
    }
    @Test void actualQueryPlansUseIndexesWithoutCollectionScanOrBlockingSort() {
        migration.migrate();
        var now = new Date();
        mongo.getCollection("posts").insertMany(IntStream.range(0,500).mapToObj(i->
                new Document("_id","p"+i).append("userId",i%10==0?"wanted":"other").append("createdAt",new Date(now.getTime()-i*1000L))).toList());
        mongo.getCollection("comments").insertMany(IntStream.range(0,500).mapToObj(i->
                new Document("_id","c"+i).append("postId","p"+(i%10)).append("createdAt",new Date(now.getTime()-i*1000L))).toList());
        mongo.getCollection("likes").insertMany(IntStream.range(0,500).mapToObj(i->like("l"+i,"p"+i,"u"+i)).toList());
        assertIndexedPlan("posts",new Document(),new Document("createdAt",-1).append("_id",-1),MongoQueryIndexMigration.FEED_INDEX);
        assertIndexedPlan("posts",new Document("userId","wanted"),new Document("createdAt",-1).append("_id",-1),MongoQueryIndexMigration.AUTHOR_INDEX);
        assertIndexedPlan("comments",new Document("postId","p1"),new Document("createdAt",-1).append("_id",-1),MongoQueryIndexMigration.COMMENTS_INDEX);
        assertIndexedPlan("likes",new Document("postId","p123").append("userId","u123"),null,MongoQueryIndexMigration.LIKES_INDEX);
    }
    void assertIndexedPlan(String collection,Document filter,Document sort,String expectedIndex) {
        var query = new Document("find",collection).append("filter",filter).append("limit",20);
        if(sort!=null)query.append("sort",sort);
        var explain = mongo.getDb().runCommand(new Document("explain",query).append("verbosity","executionStats"));
        var plan = explain.get("queryPlanner",Document.class).get("winningPlan",Document.class);
        assertTrue(contains(plan,"indexName",expectedIndex),plan.toJson());
        assertFalse(contains(plan,"stage","COLLSCAN"),plan.toJson());
        assertFalse(contains(plan,"stage","SORT"),plan.toJson());
        assertTrue(explain.get("executionStats",Document.class).get("totalDocsExamined",Number.class).longValue()<=20,explain.toJson());
    }
    boolean contains(Object node,String key,String value) {
        if(node instanceof Document doc) {
            if(value.equals(doc.get(key)))return true;
            return doc.values().stream().anyMatch(child->contains(child,key,value));
        }
        if(node instanceof List<?> list)return list.stream().anyMatch(child->contains(child,key,value));
        return false;
    }
}
