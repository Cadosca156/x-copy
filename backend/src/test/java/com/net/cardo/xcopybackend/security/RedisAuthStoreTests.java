package com.net.cardo.xcopybackend.security;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class RedisAuthStoreTests {
    @Test void redisEnforcesAtomicConsumptionSharedLimitsAndNonResurrectingResends() throws Exception {
        var factory=new LettuceConnectionFactory(System.getenv().getOrDefault("REDIS_HOST","localhost"),
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT","6379")));
        factory.afterPropertiesSet();factory.start();
        var redis=new StringRedisTemplate(factory);redis.afterPropertiesSet();
        var first=new EphemeralAuthStore(redis);var second=new EphemeralAuthStore(redis);
        String key="security:test:"+UUID.randomUUID();
        var executor=Executors.newFixedThreadPool(8);
        try {
            first.put(key,"code",Duration.ofSeconds(30));
            var tasks=new ArrayList<Callable<Boolean>>();
            for(int i=0;i<16;i++) tasks.add(()->second.consume(key,"code"));
            int successes=0;for(var result:executor.invokeAll(tasks))if(result.get())successes++;
            assertEquals(1,successes);assertFalse(first.replace(key,"code","new-code"));assertNull(first.get(key));
            first.put(key,"bound-code",Duration.ofSeconds(30));assertEquals("bound-code",second.take(key));assertNull(first.take(key));
            first.put(key,"old",Duration.ofSeconds(30));
            assertTrue(second.replace(key,"old","new"));assertFalse(first.consume(key,"old"));assertTrue(first.consume(key,"new"));
            first.limit(key,2,Duration.ofSeconds(30));second.limit(key,2,Duration.ofSeconds(30));
            assertEquals(429,assertThrows(ResponseStatusException.class,()->first.limit(key,2,Duration.ofSeconds(30))).getStatusCode().value());
        } finally {
            executor.shutdownNow();redis.delete(key);redis.delete("security:limit:"+EphemeralAuthStore.digest(key));factory.destroy();
        }
    }
}
