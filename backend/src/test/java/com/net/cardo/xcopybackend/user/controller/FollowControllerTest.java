package com.net.cardo.xcopybackend.user.controller;

import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import com.net.cardo.xcopybackend.user.service.FollowService;
import com.net.cardo.xcopybackend.post.service.PostService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowControllerTest {
    private final FollowService follows = mock(FollowService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PostService posts = mock(PostService.class);
    private final FollowController controller = new FollowController(follows, users, posts);
    private final UUID viewer = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken session() {
        var user = new User();
        user.setId(viewer);
        when(users.findByEmail("me@example.test")).thenReturn(Optional.of(user));
        return new UsernamePasswordAuthenticationToken("me@example.test", null, List.of());
    }

    @Test
    void followingFeedUsesOnlyAuthenticatedUsersSubscriptions() {
        var session = session();
        when(follows.followingIds(viewer)).thenReturn(List.of(target));
        when(posts.getPostsByAuthors(List.of(target.toString()), viewer.toString(),0,20)).thenReturn(List.of());
        assertEquals(List.of(), controller.followingPosts(session,0,20));
        verify(posts).getPostsByAuthors(List.of(target.toString()), viewer.toString(),0,20);
        verify(posts, never()).getPosts(any());
    }

    @Test
    void followsAndUnfollowsOnBehalfOfAuthenticatedUser() {
        var session = session();
        controller.follow(target, session);
        controller.unfollow(target, session);
        verify(follows).follow(viewer, target);
        verify(follows).unfollow(viewer, target);
    }

    @Test
    void googleSessionResolvesSameAccount() {
        session();
        var oidc = mock(OidcUser.class);
        when(oidc.getEmail()).thenReturn("me@example.test");
        controller.follow(target, new UsernamePasswordAuthenticationToken(oidc, null, List.of()));
        verify(follows).follow(viewer, target);
    }

    @Test
    void missingOrDeletedSessionCannotChangeSubscriptions() {
        assertThrows(ResponseStatusException.class, () -> controller.follow(target, null));
        var deleted = new UsernamePasswordAuthenticationToken("deleted@example.test", null, List.of());
        assertThrows(ResponseStatusException.class, () -> controller.follow(target, deleted));
        verifyNoInteractions(follows, posts);
    }
}
