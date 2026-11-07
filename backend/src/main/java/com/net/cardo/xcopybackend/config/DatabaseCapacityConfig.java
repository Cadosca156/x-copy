package com.net.cardo.xcopybackend.config;

import java.util.concurrent.TimeUnit;
import io.lettuce.core.ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
import org.springframework.context.annotation.*;

@Configuration
public class DatabaseCapacityConfig {
    @Bean MongoClientSettingsBuilderCustomizer mongoCapacity(
            @Value("${app.mongodb.max-pool-size:16}") int poolSize,
            @Value("${app.mongodb.operation-timeout-ms:5000}") long timeoutMs) {
        if(poolSize<1 || timeoutMs<1)throw new IllegalArgumentException("Invalid MongoDB capacity");
        return builder->builder.timeout(timeoutMs,TimeUnit.MILLISECONDS)
                .applyToConnectionPoolSettings(pool->pool.maxSize(poolSize).minSize(0).maxConnecting(2))
                .applyToClusterSettings(cluster->cluster.serverSelectionTimeout(2,TimeUnit.SECONDS))
                .applyToSocketSettings(socket->socket.connectTimeout(2,TimeUnit.SECONDS));
    }
    @Bean LettuceClientOptionsBuilderCustomizer redisCapacity() {
        // Customize Boot's options builder, retaining its connect-timeout/SSL settings.
        return builder->builder.requestQueueSize(64)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
    }
}
