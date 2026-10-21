package com.net.cardo.xcopybackend.post.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaStorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp",
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/quicktime", "mov"
    );

    private final Path uploadDirectory;

    public MediaStorageService(@Value("${app.media.upload-dir:uploads/posts}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public StoredMedia store(MultipartFile file) {
        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);

        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Підтримуються лише зображення JPEG, PNG, GIF, WebP та відео MP4, WebM, MOV"
            );
        }

        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), uploadDirectory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не вдалося зберегти медіафайл",
                    exception
            );
        }

        return new StoredMedia("/media/posts/" + fileName, contentType);
    }

    public void delete(String mediaUrl) {
        if (mediaUrl == null || !mediaUrl.startsWith("/media/posts/")) {
            return;
        }

        String fileName = mediaUrl.substring("/media/posts/".length());
        Path file = uploadDirectory.resolve(fileName).normalize();
        if (file.getParent().equals(uploadDirectory)) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // The post can still be deleted if its file was removed externally.
            }
        }
    }

    public record StoredMedia(String url, String contentType) {
    }
}
