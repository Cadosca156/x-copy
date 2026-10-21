package com.net.cardo.xcopybackend.post.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class FeedCacheService {

    public static final String CACHE_NAME = "post-feed";

    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public void invalidate() {
        // The annotation clears all user-specific feed entries in Redis.
    }
}
