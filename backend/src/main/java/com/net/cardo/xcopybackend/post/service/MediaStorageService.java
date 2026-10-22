package com.net.cardo.xcopybackend.post.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaStorageService {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/gif", "gif",
            "image/webp", "webp", "video/mp4", "mp4", "video/webm", "webm",
            "video/quicktime", "mov"
    );
    private final Path uploadDirectory;

    public MediaStorageService(@Value("${app.media.upload-dir:uploads/posts}") String directory) {
        uploadDirectory = Path.of(directory).toAbsolutePath().normalize();
    }

    public StoredMedia store(MultipartFile file) {
        String contentType = file.getContentType();
        String extension = contentType == null ? null : EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Підтримуються лише JPEG, PNG, GIF, WebP та відео MP4, WebM, MOV");
        }
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, uploadDirectory.resolve(name));
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не вдалося зберегти медіафайл", exception);
        }
        return new StoredMedia("/media/posts/" + name, contentType);
    }

    public void delete(String url) {
        if (url == null || !url.startsWith("/media/posts/")) return;
        Path file = uploadDirectory.resolve(url.substring("/media/posts/".length())).normalize();
        if (!uploadDirectory.equals(file.getParent())) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // A post can be removed even if its file is already unavailable.
        }
    }

    public record StoredMedia(String url, String contentType) {}
}
