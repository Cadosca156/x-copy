package com.net.cardo.xcopybackend.post.service;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PostCleanupTest {
    @TempDir Path directory;
    @Test void retriesHaveBoundedExponentialBackoff() {
        assertEquals(5,PostCleanupService.backoffSeconds(1));assertEquals(10,PostCleanupService.backoffSeconds(2));
        assertEquals(3600,PostCleanupService.backoffSeconds(20));assertEquals(3600,PostCleanupService.backoffSeconds(Integer.MAX_VALUE));
    }
    @Test void strictMediaDeletionDistinguishesMissingFilesFromFailure() throws Exception {
        var media=new MediaStorageService(directory.toString());
        assertDoesNotThrow(()->media.deleteOrThrow("/media/posts/missing.png"));
        Files.createDirectory(directory.resolve("not-a-file"));
        assertThrows(IOException.class,()->media.deleteOrThrow("/media/posts/not-a-file"));
        assertThrows(IOException.class,()->media.deleteOrThrow("/media/posts/../outside"));
        assertThrows(IOException.class,()->media.deleteOrThrow("/media/posts/.quota-v1/total"));
    }
}
