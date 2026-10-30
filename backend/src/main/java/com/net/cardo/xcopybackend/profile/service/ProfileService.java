package com.net.cardo.xcopybackend.profile.service;

import com.net.cardo.xcopybackend.user.dto.request.UpdateProfileRequest;
import com.net.cardo.xcopybackend.user.dto.response.ProfileResponse;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.net.cardo.xcopybackend.post.service.MediaStorageService;
import com.net.cardo.xcopybackend.post.service.FeedCacheService;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final MediaStorageService mediaStorageService;
    private final FeedCacheService feedCacheService;

    public ProfileResponse updateAvatar(String email, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 5L * 1024 * 1024) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose a non-empty image up to 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(contentType)) {
            throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "Choose JPEG, PNG, WebP or GIF");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        String previousAvatar = user.getAvatarUrl();
        MediaStorageService.StoredMedia media = mediaStorageService.store(file, user.getId());
        user.setAvatarUrl(media.url());
        User saved;
        try {
            saved = userRepository.save(user);
        } catch (RuntimeException exception) {
            mediaStorageService.delete(media.url());
            throw exception;
        }
        mediaStorageService.delete(previousAvatar);
        feedCacheService.invalidate();
        return new ProfileResponse(saved.getId(), saved.getUsername(), saved.getDisplayName(), saved.getAvatarUrl());
    }

    public ProfileResponse getMyProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }

    @Transactional
    public ProfileResponse updateProfile(
            String email,
            UpdateProfileRequest request
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(NOT_FOUND, "User not found")
                );

        if (request.username() == null && request.displayName() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Provide a username or display name");
        }

        // PATCH validates only fields that are being changed. Legacy usernames
        // must not prevent updating the display name.
        String username = user.getUsername();
        if (request.username() != null && !request.username().equals(username)) {
            username = request.username().trim().toLowerCase(java.util.Locale.ROOT);
            if (!username.matches("^[a-z0-9_]{3,30}$")) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid username");
            }
            if (!username.equalsIgnoreCase(user.getUsername())
                    && userRepository.existsByUsernameIgnoreCase(username)) {
                throw new ResponseStatusException(CONFLICT, "Username already taken");
            }
        }

        String displayName = request.displayName() == null
                ? user.getDisplayName() : request.displayName().trim();

        if (request.displayName() != null && (displayName.isBlank() || displayName.length() > 50)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Invalid display name"
            );
        }

        user.setUsername(username);
        user.setDisplayName(displayName);

        User savedUser = userRepository.save(user);
        feedCacheService.invalidate();

        return new ProfileResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getDisplayName(),
                savedUser.getAvatarUrl()
        );


    }
}
