package com.net.cardo.xcopybackend.post.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class FeedCacheService {
    public static final String CACHE_NAME = "post-feed";

    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public void invalidate() {
        // Clear cached feeds after changes to posts or their authors.
    }
}
