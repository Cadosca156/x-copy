package com.net.cardo.xcopybackend.post.service;

import java.nio.file.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class MediaQuotaBoundaryTest {
    @TempDir Path directory;
    private byte[] png() {return new byte[]{(byte)137,80,78,71,13,10,26,10,0,1,2,3};}
    private MockMultipartFile file() {return new MockMultipartFile("file","x.png","image/png",png());}
    @Test void byteQuotasCoverMultipleFilesAndCannotBeBypassedWithAnotherAccount() {
        var storage = new MediaStorageService(directory.toString());
        ReflectionTestUtils.setField(storage,"ownerMaxBytes",12L);
        ReflectionTestUtils.setField(storage,"totalMaxBytes",24L);
        UUID first = UUID.randomUUID();storage.store(file(),first);
        assertEquals(507,assertThrows(ResponseStatusException.class,()->storage.store(file(),first)).getStatusCode().value());
        storage.store(file(),UUID.randomUUID());
        assertEquals(507,assertThrows(ResponseStatusException.class,()->storage.store(file(),UUID.randomUUID())).getStatusCode().value());
    }
    @Test void oversizedUploadsAreRejectedBeforeOpeningAnInputStream() {
        var storage = new MediaStorageService(directory.toString());
        var large = new MockMultipartFile("file","x.png","image/png",png()) {
            @Override public long getSize() {return 26L*1024*1024;}
            @Override public java.io.InputStream getInputStream() {throw new AssertionError("Upload must not be read");}
        };
        assertEquals(413,assertThrows(ResponseStatusException.class,()->storage.store(large,UUID.randomUUID())).getStatusCode().value());
    }
    @Test void dishonestUploadLengthIsRejectedAndPartialFileIsRemoved() throws Exception {
        var storage = new MediaStorageService(directory.toString());
        var dishonest = new MockMultipartFile("file","x.png","image/png",png()) {
            @Override public long getSize() {return 11;}
        };
        assertEquals(413,assertThrows(ResponseStatusException.class,()->storage.store(dishonest,UUID.randomUUID())).getStatusCode().value());
        try(var files = Files.walk(directory)) {assertEquals(0,files.filter(p->p.toString().endsWith(".png")).count());}
    }
}
