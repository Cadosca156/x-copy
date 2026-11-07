package com.net.cardo.xcopybackend.post.repository;
import com.net.cardo.xcopybackend.post.service.FeedCursor;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostWindowQueryTest {
    @Test void deepPositionNeverBecomesSkipAndQueryHasStrictTieBreaker() {
        var date=LocalDateTime.of(2026,11,6,12,0);
        var query=PostWindowRepositoryImpl.query(null,null,new FeedCursor.Position(date,"post-id"),51);
        assertEquals(0,query.getSkip());assertEquals(51,query.getLimit());
        String filter=query.getQueryObject().toString();assertTrue(filter.contains("$lt"));assertTrue(filter.contains("post-id"));
        assertEquals(-1,query.getSortObject().get("createdAt"));assertEquals(-1,query.getSortObject().get("id"));
        assertThrows(IllegalArgumentException.class,()->PostWindowRepositoryImpl.query(null,null,null,52));
    }
}
