package com.net.cardo.xcopybackend.post.service;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class FeedCursorTest {
    @Test void roundTripsDateIdAndScopeAtDatabasePrecision() {
        var position=new FeedCursor.Position(LocalDateTime.of(2026,11,6,12,0,1,123000000),"0123456789abcdef01234567");
        String token=FeedCursor.encode("all",position);
        assertEquals(position,FeedCursor.decode("all",token));assertNull(FeedCursor.decode("all",null));
        assertThrows(ResponseStatusException.class,()->FeedCursor.decode("author:other",token));
    }
    @Test void malformedOversizedUnknownVersionAndTrailingDataAreRejected() {
        for(String cursor:new String[]{"","?bad","a".repeat(601),"AA","Ag"})
            assertEquals(400,assertThrows(ResponseStatusException.class,()->FeedCursor.decode("all",cursor)).getStatusCode().value());
        String token=FeedCursor.encode("all",new FeedCursor.Position(LocalDateTime.of(2026,1,1,0,0),"id"));
        assertThrows(ResponseStatusException.class,()->FeedCursor.decode("all",token+"AA"));
    }
}
