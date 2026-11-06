package com.net.cardo.xcopybackend.config;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import static org.junit.jupiter.api.Assertions.*;

class MediaWebConfigTest {
    @TempDir Path root;
    @Test void quotaAndUncommittedFilesCannotBeServed() throws Exception {
        Files.createDirectories(root.resolve(".staging"));Files.writeString(root.resolve(".staging/upload.part"),"private");
        Files.createDirectories(root.resolve("owner"));Files.writeString(root.resolve("owner/image.png"),"public");
        var resolver=new MediaWebConfig.PublicMediaResolver();var location=new FileSystemResource(root.toString()+"/");
        assertNull(resolver.getResource(".staging/upload.part",location));
        assertNull(resolver.getResource(".quota-v1/total",location));
        assertNotNull(resolver.getResource("owner/image.png",location));
    }
}
