package com.net.cardo.xcopybackend.config;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MongoIndexKeyOrderTest {
    @Test void compoundKeyOrderIsPartOfTheIndexContract() {
        var expected = new Document("userId",1).append("createdAt",-1);
        assertTrue(MongoQueryIndexMigration.sameKeyOrder(expected,new Document("userId",1).append("createdAt",-1)));
        assertFalse(MongoQueryIndexMigration.sameKeyOrder(expected,new Document("createdAt",-1).append("userId",1)));
        assertFalse(MongoQueryIndexMigration.sameKeyOrder(expected,new Document("userId",1).append("createdAt",1)));
        assertFalse(MongoQueryIndexMigration.sameKeyOrder(expected,null));
    }
}
