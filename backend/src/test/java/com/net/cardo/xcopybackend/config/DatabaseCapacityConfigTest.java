package com.net.cardo.xcopybackend.config;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseCapacityConfigTest {
    @Test void customizersApplyFiniteDriverBudgets() {
        var config=new DatabaseCapacityConfig();var mongo=com.mongodb.MongoClientSettings.builder();
        config.mongoCapacity(16,5000).customize(mongo);var settings=mongo.build();
        assertEquals(16,settings.getConnectionPoolSettings().getMaxSize());
        assertEquals(5000L,settings.getTimeout(TimeUnit.MILLISECONDS));
        assertEquals(2000,settings.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS));
        var redis=io.lettuce.core.ClientOptions.builder();config.redisCapacity().customize(redis);
        assertEquals(64,redis.build().getRequestQueueSize());
        assertEquals(io.lettuce.core.ClientOptions.DisconnectedBehavior.REJECT_COMMANDS,redis.build().getDisconnectedBehavior());
    }
}
