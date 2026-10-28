package com.net.cardo.xcopybackend.user.service;

import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowServiceTest {
    private final UserFollowRepository follows = mock(UserFollowRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final FollowService service = new FollowService(follows, users);
    private final UUID viewer = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @Test
    void subscriptionsPersistAndCanBeRemoved() {
        when(users.existsById(target)).thenReturn(true);
        service.follow(viewer, target);
        verify(follows).addFollow(viewer + ":" + target, viewer, target);
        service.unfollow(viewer, target);
        verify(follows).deleteByFollowerIdAndFollowedId(viewer, target);
    }

    @Test
    void cannotFollowSelfOrMissingUser() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.follow(viewer, viewer)).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.follow(viewer, target)).getStatusCode());
        verifyNoInteractions(follows);
    }

    @Test
    void suggestionsRequestThreeOtherUsersAndIncludeSavedFollowingStatus() {
        var user = new User();
        user.setId(target);
        user.setUsername("real_user");
        user.setDisplayName("Real user");
        user.setAvatarUrl("/avatar.png");
        when(follows.findFollowingIds(viewer)).thenReturn(List.of(target));
        when(users.findFollowSuggestions(viewer, PageRequest.of(0, 3))).thenReturn(List.of(user));
        var suggestions = service.suggestions(viewer);
        assertEquals(1, suggestions.size());
        assertEquals("real_user", suggestions.get(0).username());
        assertEquals("/avatar.png", suggestions.get(0).avatarUrl());
        assertTrue(suggestions.get(0).following());
        verify(users).findFollowSuggestions(viewer, PageRequest.of(0, 3));
    }
}
