package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Post;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FeedCacheService {
    public static final String CACHE_NAME = "post-pages-v2";
    private final StringRedisTemplate redis;
    private final CacheManager caches;
    private final Object[] stripes = new Object[64];

    public FeedCacheService(StringRedisTemplate redis, CacheManager caches) {
        this.redis = redis; this.caches = caches;
        Arrays.setAll(stripes, i -> new Object());
    }

    /** Only post membership/content is cached; viewer metadata is loaded afterwards. */
    public List<Post> load(String scope, int page, int size, Supplier<List<Post>> loader) {
        return load(scope,"offset:"+page,size,loader);
    }

    public List<Post> load(String scope, String position, int size, Supplier<List<Post>> loader) {
        synchronized (stripes[Math.floorMod(Objects.hash(scope,position,size),stripes.length)]) {
            String generation;
            try {
                generation = redis.opsForValue().get(versionKey(scope));
                if(generation == null) {
                    redis.opsForValue().setIfAbsent(versionKey(scope),UUID.randomUUID().toString());
                    generation=redis.opsForValue().get(versionKey(scope));
                }
            }
            catch (RuntimeException unavailable) { return loader.get(); }
            if(generation == null)return loader.get();
            String key = scope + ":" + generation + ":" + position + ":" + size;
            org.springframework.cache.Cache cache;
            try {
                cache = caches.getCache(CACHE_NAME);
                var entry = cache == null ? null : cache.get(key);
                if (entry != null) {
                    @SuppressWarnings("unchecked") var posts = (List<Post>) entry.get();
                    return posts;
                }
            } catch (RuntimeException unavailable) { return loader.get(); }
            List<Post> posts = loader.get();
            try { if (cache != null) cache.put(key,new ArrayList<>(posts)); }
            catch (RuntimeException unavailable) { /* Cache failure must not hide a successful read. */ }
            return posts;
        }
    }

    public void invalidate(String authorId) {
        // MSET changes both scopes atomically; old generations expire with the page TTL.
        String generation = UUID.randomUUID().toString();
        try {
            redis.opsForValue().multiSet(Map.of(versionKey("all"),generation,
                    versionKey("author:"+authorId),generation));
        } catch (org.springframework.dao.DataAccessException unavailable) {
            // The database mutation has already committed. Cached pages expire after 30 seconds.
            org.slf4j.LoggerFactory.getLogger(FeedCacheService.class)
                    .warn("Post cache invalidation unavailable; waiting for page TTL");
        }
    }
    private String versionKey(String scope) { return "xcopy:post-page-generation:v2:"+scope; }
}
