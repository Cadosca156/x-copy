package com.net.cardo.xcopybackend.post.service;

import com.mongodb.WriteConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/** Durable deletion jobs live in the tombstoned post until every step succeeds. */
@Service
public class PostCleanupService implements InitializingBean {
    private static final Logger log=LoggerFactory.getLogger(PostCleanupService.class);
    private final MongoTemplate mongo;
    private final MediaStorageService media;
    private final FeedCacheService cache;
    private final Clock clock;
    private final Map<String,Object> scanPositions=new HashMap<>();
    @Value("${app.post-cleanup.jobs-per-run:5}") private int jobsPerRun=5;
    @Value("${app.post-cleanup.orphan-batch-size:100}") private int orphanBatchSize=100;
    @Autowired public PostCleanupService(MongoTemplate mongo,MediaStorageService media,FeedCacheService cache) {
        this(mongo,media,cache,Clock.systemUTC());
    }
    PostCleanupService(MongoTemplate mongo,MediaStorageService media,FeedCacheService cache,Clock clock) {
        this.mongo=mongo;this.media=media;this.cache=cache;this.clock=clock;
    }
    private MongoCollection<Document> posts(){return mongo.getCollection("posts").withWriteConcern(WriteConcern.JOURNALED).withReadPreference(ReadPreference.primary());}
    @Override public void afterPropertiesSet() {
        if(jobsPerRun<1 || jobsPerRun>50 || orphanBatchSize<1 || orphanBatchSize>500)
            throw new IllegalArgumentException("Invalid post cleanup batch limits");
        mongo.getDb().withTimeout(120,TimeUnit.SECONDS).getCollection("posts").createIndex(
                new Document("deletionRequested",1).append("cleanupNextAttemptAt",1).append("_id",1),
                new IndexOptions().name("posts_cleanup_due_v1"));
    }
    public synchronized void runBatch() {
        try {
            for(int i=0;i<jobsPerRun;i++) {
                Document claim=claimNext();if(claim==null)break;
                finish(claim);
            }
            for(String collection:List.of("likes","comments"))try {repairOrphans(collection);}
            catch(RuntimeException unavailable) {log.warn("Orphan {} cleanup deferred: {}",collection,unavailable.getClass().getSimpleName());}
        } catch(RuntimeException unavailable) {
            log.warn("Post cleanup batch deferred: {}",unavailable.getClass().getSimpleName());
        }
    }
    Document claimNext() {
        Instant now=clock.instant();String token=UUID.randomUUID().toString();
        var available=new Document("$or",List.of(new Document("cleanupLeaseUntil",null),
                new Document("cleanupLeaseUntil",new Document("$lte",Date.from(now)))));
        var filter=new Document("deletionRequested",true).append("cleanupNextAttemptAt",new Document("$lte",Date.from(now)))
                .append("$and",List.of(available));
        var update=new Document("$set",new Document("cleanupLeaseToken",token).append("cleanupLeaseUntil",Date.from(now.plusSeconds(30))))
                .append("$inc",new Document("cleanupAttempts",1));
        return posts().findOneAndUpdate(filter,update,new FindOneAndUpdateOptions()
                .sort(new Document("cleanupNextAttemptAt",1).append("_id",1)).returnDocument(ReturnDocument.AFTER));
    }
    private Document owned(Document claim) {
        return new Document("_id",claim.get("_id")).append("deletionRequested",true)
                .append("cleanupLeaseToken",claim.getString("cleanupLeaseToken"));
    }
    void finish(Document claim) {
        String postId=claim.get("_id").toString();
        try {
            var children=new Document("postId",postId);
            mongo.getCollection("likes").withWriteConcern(WriteConcern.JOURNALED).deleteMany(children);
            mongo.getCollection("comments").withWriteConcern(WriteConcern.JOURNALED).deleteMany(children);
            media.deleteOrThrow(claim.getString("mediaUrl"));
            cache.invalidate(claim.getString("userId"));
            // Retain the only durable cleanup reference until all previous steps succeed.
            posts().deleteOne(owned(claim));
        } catch(Exception failure) {
            int attempts=((Number)claim.get("cleanupAttempts")).intValue();
            var update=new Document("$set",new Document("cleanupNextAttemptAt",Date.from(clock.instant().plusSeconds(backoffSeconds(attempts)))))
                    .append("$unset",new Document("cleanupLeaseToken","").append("cleanupLeaseUntil",""));
            posts().updateOne(owned(claim),update);
            log.warn("Post cleanup deferred; post={}, attempt={}, error={}",postId,attempts,failure.getClass().getSimpleName());
        }
    }
    static long backoffSeconds(int attempts){return Math.min(3600L,5L*(1L<<Math.min(10,Math.max(0,attempts-1))));}

    /** A bounded rolling scan also catches a writer that committed after cascade cleanup. */
    void repairOrphans(String name) {
        if(!mongo.collectionExists(name)){scanPositions.remove(name);return;}
        Object previous=scanPositions.get(name);
        var collection=mongo.getCollection(name).withWriteConcern(WriteConcern.JOURNALED).withReadPreference(ReadPreference.primary());
        var find=collection.find().projection(new Document("_id",1).append("postId",1))
                .sort(new Document("_id",1)).hintString("_id_").limit(orphanBatchSize+1).maxTime(2,TimeUnit.SECONDS);
        // Index bounds preserve BSON type order, including legacy String and ObjectId IDs.
        if(previous!=null)find=find.min(new Document("_id",previous));
        var page=new ArrayList<Document>();
        try(var cursor=find.iterator()) {
            while(cursor.hasNext() && page.size()<orphanBatchSize) {
                var row=cursor.next();if(previous!=null && previous.equals(row.get("_id")))continue;
                page.add(row);
            }
        }
        if(page.isEmpty()){scanPositions.remove(name);return;}
        var references=new HashSet<String>();
        for(var row:page)if(row.get("postId") instanceof String id && !id.isBlank())references.add(id);
        var candidates=new ArrayList<Object>();
        for(String id:references){candidates.add(id);if(ObjectId.isValid(id))candidates.add(new ObjectId(id));}
        var existing=new HashSet<String>();
        if(!candidates.isEmpty())try(var parents=posts().find(new Document("_id",new Document("$in",candidates)))
                .projection(new Document("_id",1)).maxTime(2,TimeUnit.SECONDS).iterator()) {
            while(parents.hasNext())existing.add(parents.next().get("_id").toString());
        }
        var orphanIds=new ArrayList<Object>();
        for(var row:page)if(row.get("postId") instanceof String id && !id.isBlank() && !existing.contains(id))orphanIds.add(row.get("_id"));
        if(!orphanIds.isEmpty()) {
            long removed=collection.deleteMany(new Document("_id",new Document("$in",orphanIds))).getDeletedCount();
            log.info("Orphan {} cleanup removed {} records",name,removed);
        }
        if(page.size()<orphanBatchSize)scanPositions.remove(name);
        else scanPositions.put(name,page.get(page.size()-1).get("_id"));
    }
}
