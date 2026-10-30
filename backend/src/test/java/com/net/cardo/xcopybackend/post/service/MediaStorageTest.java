package com.net.cardo.xcopybackend.post.service;
import java.nio.file.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class MediaStorageTest {
    @TempDir Path directory;
    private MockMultipartFile png() {
        return new MockMultipartFile("file","ignored.exe","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10,0,1,2,3});
    }
    @Test void rejectsSpoofedTypeWithoutWritingAFile() throws Exception {
        var storage=new MediaStorageService(directory.toString());
        var file=new MockMultipartFile("file","photo.png","image/png","<script>bad</script>".getBytes());
        assertEquals(415,assertThrows(ResponseStatusException.class,()->storage.store(file,UUID.randomUUID())).getStatusCode().value());
        try(var files=Files.walk(directory)){assertFalse(files.anyMatch(p->p.toString().endsWith(".png")));}
    }
    @Test void ownerAndGlobalQuotasCannotBeBypassedWithAnotherFileName() {
        var storage=new MediaStorageService(directory.toString());UUID owner=UUID.randomUUID();
        ReflectionTestUtils.setField(storage,"ownerMaxFiles",1L);
        var stored=storage.store(png(),owner);
        assertTrue(stored.url().startsWith("/media/posts/"+owner+"/"));
        assertEquals(507,assertThrows(ResponseStatusException.class,()->storage.store(png(),owner)).getStatusCode().value());
        ReflectionTestUtils.setField(storage,"totalMaxFiles",1L);
        assertEquals(507,assertThrows(ResponseStatusException.class,()->storage.store(png(),UUID.randomUUID())).getStatusCode().value());
        storage.delete(stored.url());storage.store(png(),owner);
    }
    @Test void rejectsTraversalWhenDeleting() throws Exception {
        Path sentinel=directory.resolve("keep");Files.writeString(sentinel,"safe");
        new MediaStorageService(directory.resolve("uploads").toString()).delete("/media/posts/../keep");
        assertEquals("safe",Files.readString(sentinel));
    }
    @Test void rejectsInvalidPaginationInsteadOfLoadingAnUnboundedFeed() {
        assertEquals(20,PostService.page(0,20).getPageSize());
        assertThrows(ResponseStatusException.class,()->PostService.page(0,100000));
        assertThrows(ResponseStatusException.class,()->PostService.page(-1,20));
        assertThrows(ResponseStatusException.class,()->PostService.page(1001,20));
    }
}
