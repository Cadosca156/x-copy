package com.net.cardo.xcopybackend.config;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import java.util.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs during singleton initialization, before the application accepts requests.
 * MongoDB 6+ is required for prepareUnique/collMod; deployments use MongoDB 8.
 */
@Component
public class MongoQueryIndexMigration implements InitializingBean {
    public static final String FEED_INDEX = "posts_created_id_v1";
    public static final String AUTHOR_INDEX = "posts_user_created_id_v1";
    public static final String COMMENTS_INDEX = "comments_post_created_id_v1";
    public static final String LIKES_INDEX = "likes_post_user_unique_v1";
    public static final String ARCHIVE = "likes_duplicate_archive_v1";
    private static final Logger log = LoggerFactory.getLogger(MongoQueryIndexMigration.class);
    private final MongoTemplate mongo;

    public MongoQueryIndexMigration(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override public void afterPropertiesSet() {
        migrate();
    }

    /** Idempotent; public for verification against a disposable database. */
    public void migrate() {
        ensureIndex("posts", new Document("createdAt", -1).append("_id", -1), FEED_INDEX);
        ensureIndex("posts", new Document("userId", 1).append("createdAt", -1).append("_id", -1), AUTHOR_INDEX);
        ensureIndex("comments", new Document("postId", 1).append("createdAt", -1).append("_id", -1), COMMENTS_INDEX);
        Document index = ensureIndex("likes", new Document("postId", 1).append("userId", 1), LIKES_INDEX);
        if (Boolean.TRUE.equals(index.getBoolean("unique"))) return;

        MongoCollection<Document> likes = mongo.getCollection("likes");
        rejectInvalidLikeIds(likes);
        String indexName = index.getString("name");
        // Stop new duplicate pairs before removing any old duplicates.
        modifyLikesIndex(new Document("name", indexName).append("prepareUnique", true), false);
        long removed = archiveAndRemoveDuplicates(likes, indexName);
        modifyLikesIndex(new Document("name", indexName).append("unique", true), true);
        modifyLikesIndex(new Document("name", indexName).append("unique", true), false);
        // Like metadata is loaded live; post-page membership did not change.
        log.info("MongoDB query indexes ready; archived and removed {} duplicate likes", removed);
    }

    private Document ensureIndex(String collectionName, Document keys, String desiredName) {
        if (!mongo.collectionExists(collectionName)) mongo.createCollection(collectionName);
        var collection = mongo.getCollection(collectionName);
        List<Document> indexes = collection.listIndexes().into(new ArrayList<>());
        for (Document index : indexes) {
            if (desiredName.equals(index.getString("name")) && !sameKeyOrder(keys, index.get("key", Document.class))) {
                throw new IllegalStateException("Index " + desiredName + " has unexpected keys; inspect it before deployment");
            }
        }
        for (Document index : indexes) {
            if (sameKeyOrder(keys, index.get("key", Document.class))) {
                if (index.containsKey("partialFilterExpression") || Boolean.TRUE.equals(index.getBoolean("sparse"))
                        || Boolean.TRUE.equals(index.getBoolean("hidden")) || index.containsKey("collation")) {
                    throw new IllegalStateException("Existing index on " + collectionName + " has incompatible options; inspect it before deployment");
                }
                // Reuse an equivalent full index even if it was created manually under another name.
                return index;
            }
        }
        collection.createIndex(keys, new IndexOptions().name(desiredName));
        return new Document("name", desiredName).append("key", keys);
    }

    static boolean sameKeyOrder(Document expected, Document actual) {
        return actual != null && new ArrayList<>(expected.entrySet()).equals(new ArrayList<>(actual.entrySet()));
    }

    private void rejectInvalidLikeIds(MongoCollection<Document> likes) {
        Document notStrings = new Document("$expr", new Document("$or", List.of(
                new Document("$ne", List.of(new Document("$type", "$postId"), "string")),
                new Document("$ne", List.of(new Document("$type", "$userId"), "string")))));
        Document invalid = new Document("$or", List.of(notStrings, new Document("postId", ""), new Document("userId", "")));
        if (likes.find(invalid).projection(new Document("_id", 1)).first() != null) {
            throw new IllegalStateException("likes contains invalid postId/userId values; inspect legacy data before retrying the migration");
        }
    }

    private void modifyLikesIndex(Document options, boolean dryRun) {
        Document command = new Document("collMod", "likes").append("index", options);
        if (dryRun) command.append("dryRun", true);
        mongo.getDb().runCommand(command);
    }

    private long archiveAndRemoveDuplicates(MongoCollection<Document> likes, String indexName) {
        var archive = mongo.getCollection(ARCHIVE).withWriteConcern(WriteConcern.JOURNALED);
        String previousPost = null, previousUser = null;
        long removed = 0;
        // Adjacent equal pairs come from the existing index; no giant $push array or full Java list.
        try (var cursor = likes.find().sort(new Document("postId", 1).append("userId", 1))
                .hintString(indexName).batchSize(256).iterator()) {
            while (cursor.hasNext()) {
                Document original = cursor.next();
                String postId = original.getString("postId"), userId = original.getString("userId");
                if (postId.equals(previousPost) && userId.equals(previousUser)) {
                    archiveDuplicate(archive, original);
                    // Archive acknowledgement precedes deletion. Failure leaves a retryable archive.
                    removed += likes.deleteOne(original).getDeletedCount();
                } else {
                    previousPost = postId;
                    previousUser = userId;
                }
            }
        }
        return removed;
    }

    static void archiveDuplicate(MongoCollection<Document> archive, Document original) {
        Document backup = new Document("_id", original.get("_id"))
                .append("sourceCollection", "likes").append("reason", "duplicate postId/userId")
                .append("archivedAt", new Date()).append("document", new Document(original));
        archive.updateOne(new Document("_id", original.get("_id")), new Document("$setOnInsert", backup),
                new UpdateOptions().upsert(true));
        Document persisted = archive.find(new Document("_id", original.get("_id"))).first();
        if (persisted == null || !original.equals(persisted.get("document", Document.class))) {
            throw new IllegalStateException("Archived like snapshot does not match the active document; inspect it before retrying");
        }
    }
}
