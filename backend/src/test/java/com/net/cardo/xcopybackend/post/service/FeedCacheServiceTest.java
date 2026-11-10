package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Post;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.core.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedCacheServiceTest {
    private final StringRedisTemplate redis=mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked") private final ValueOperations<String,String> values=mock(ValueOperations.class);
    private final Map<String,String> generations=new ConcurrentHashMap<>();
    private final FeedCacheService service=new FeedCacheService(redis,new ConcurrentMapCacheManager(FeedCacheService.CACHE_NAME));
    @BeforeEach void setup() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(i->generations.get(i.getArgument(0)));
        when(values.setIfAbsent(anyString(),anyString())).thenAnswer(i->generations.putIfAbsent(i.getArgument(0),i.getArgument(1))==null);
        doAnswer(i->{generations.putAll(i.getArgument(0));return null;}).when(values).multiSet(anyMap());
    }
    @Test void sharesPostPageAndInvalidatesOnlyGlobalAndChangedAuthor() {
        var loads=new AtomicInteger();
        java.util.function.Supplier<List<Post>> loader=()->{loads.incrementAndGet();return List.of(new Post());};
        service.load("all",0,20,loader);service.load("all",0,20,loader);
        service.load("author:a",0,20,loader);service.load("author:b",0,20,loader);
        assertEquals(3,loads.get());
        service.invalidate("a");
        service.load("all",0,20,loader);service.load("author:a",0,20,loader);service.load("author:b",0,20,loader);
        assertEquals(5,loads.get());
        service.load("author:b",1,20,loader);assertEquals(6,loads.get());
    }
    @Test void concurrentOldLoadCannotPopulateTheNewGeneration() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var pool=Executors.newSingleThreadExecutor();
        try {
            var old=pool.submit(()->service.load("all",0,20,()->{
                entered.countDown();try { assertTrue(release.await(5,TimeUnit.SECONDS)); }
                catch(InterruptedException e){throw new RuntimeException(e);}return List.of(new Post());
            }));
            assertTrue(entered.await(5,TimeUnit.SECONDS));service.invalidate("a");release.countDown();old.get(5,TimeUnit.SECONDS);
            var loads=new AtomicInteger();
            assertEquals(List.of(),service.load("all",0,20,()->{loads.incrementAndGet();return List.of();}));
            assertEquals(1,loads.get());
        } finally { release.countDown();pool.shutdownNow(); }
    }
    @Test void readCacheFailureFallsBackButRepositoryFailureIsNotRetried() {
        when(values.get(anyString())).thenThrow(new IllegalStateException("Redis unavailable"));
        var loads=new AtomicInteger();
        assertEquals(List.of(),service.load("all",0,20,()->{loads.incrementAndGet();return List.of();}));
        assertEquals(1,loads.get());
        var failure=new IllegalStateException("database unavailable");
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.load("all",0,20,()->{throw failure;})));
    }
    @Test void losingGenerationDoesNotResurrectOldInitialEntries() {
        service.load("all",0,20,()->List.of(new Post()));generations.clear();
        assertEquals(List.of(),service.load("all",0,20,List::of));
    }

    @Test void invalidationOutageDoesNotReportAnAlreadyCommittedPostAsFailed() {
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("Redis unavailable"))
                .when(values).multiSet(anyMap());
        assertDoesNotThrow(()->service.invalidate("author"));
    }
}
