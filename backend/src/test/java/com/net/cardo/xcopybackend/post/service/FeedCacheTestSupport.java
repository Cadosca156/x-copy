package com.net.cardo.xcopybackend.post.service;

import java.util.function.Supplier;
import static org.mockito.Mockito.*;

final class FeedCacheTestSupport {
    static FeedCacheService passthrough() {
        return mock(FeedCacheService.class,invocation -> {
            if(invocation.getMethod().getName().equals("load"))
                return ((Supplier<?>)invocation.getArgument(3)).get();
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }
}
