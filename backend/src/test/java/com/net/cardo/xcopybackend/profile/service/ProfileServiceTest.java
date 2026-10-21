package com.net.cardo.xcopybackend.profile.service;

import com.net.cardo.xcopybackend.post.service.FeedCacheService;
import com.net.cardo.xcopybackend.post.service.MediaStorageService;
import com.net.cardo.xcopybackend.user.dto.request.UpdateProfileRequest;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileServiceTest {
    private UserRepository repository;
    private FeedCacheService cache;
    private ProfileService service;
    private User user;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        cache = mock(FeedCacheService.class);
        service = new ProfileService(repository, mock(MediaStorageService.class), cache);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("Старе ім’я");
        user.setDisplayName("Old name");
        user.setAvatarUrl("/media/posts/avatar.png");
        when(repository.findByEmail("user@example.test")).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void changesDisplayNameWithoutChangingLegacyUsername() {
        var result = service.updateProfile("user@example.test", new UpdateProfileRequest(null, "  Нове ім’я  "));
        assertEquals("Нове ім’я", result.displayName());
        assertEquals("Старе ім’я", result.username());
        assertEquals("/media/posts/avatar.png", result.avatarUrl());
        verify(cache).invalidate();
        verify(repository, never()).existsByUsernameIgnoreCase(any());
    }

    @Test
    void acceptsUnchangedLegacyUsernameFromOlderClients() {
        var result = service.updateProfile("user@example.test", new UpdateProfileRequest(user.getUsername(), "New name"));
        assertEquals("New name", result.displayName());
        assertEquals("Старе ім’я", result.username());
    }

    @Test
    void rejectsNewInvalidUsername() {
        var error = assertThrows(ResponseStatusException.class, () -> service.updateProfile("user@example.test", new UpdateProfileRequest("New invalid name", "Name")));
        assertEquals(400, error.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsBlankDisplayName() {
        var error = assertThrows(ResponseStatusException.class, () -> service.updateProfile("user@example.test", new UpdateProfileRequest(null, "   ")));
        assertEquals(400, error.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsTakenUsername() {
        when(repository.existsByUsernameIgnoreCase("taken")).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class, () -> service.updateProfile("user@example.test", new UpdateProfileRequest("taken", "Name")));
        assertEquals(409, error.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void acceptsValidSetupProfile() {
        user.setUsername(null);
        user.setDisplayName(null);
        var result = service.updateProfile("user@example.test", new UpdateProfileRequest(" New_User ", "Name"));
        assertEquals("new_user", result.username());
        assertEquals("Name", result.displayName());
    }
}
