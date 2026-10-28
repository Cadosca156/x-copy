package com.net.cardo.xcopybackend.post.controller;

import com.net.cardo.xcopybackend.exception.GlobalExceptionHandler;
import com.net.cardo.xcopybackend.post.service.LikeService;
import com.net.cardo.xcopybackend.post.service.PostService;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostControllerTest {
    @Test
    void deletedUserCannotReadOrLikePostsWithAnOldSession() throws Exception {
        var users = mock(UserRepository.class);
        var posts = mock(PostService.class);
        var likes = mock(LikeService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PostController(posts, users, likes))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var principal = new UsernamePasswordAuthenticationToken("deleted@example.test", null, List.of());
        mvc.perform(get("/api/posts").principal(principal)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/posts/user/some-user").principal(principal)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posts/some-post/like").principal(principal)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/posts/some-post").principal(principal)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/posts/some-post")).andExpect(status().isUnauthorized());
        verifyNoInteractions(posts, likes);
    }

    @Test
    void deletionUsesAuthenticatedUserIdForPasswordAndGoogleSessions() throws Exception {
        var users = mock(UserRepository.class);
        var posts = mock(PostService.class);
        var user = new com.net.cardo.xcopybackend.user.entity.User();
        user.setId(java.util.UUID.randomUUID());
        when(users.findByEmail("owner@example.test")).thenReturn(java.util.Optional.of(user));
        var mvc = MockMvcBuilders.standaloneSetup(new PostController(posts, users, mock(LikeService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var password = new UsernamePasswordAuthenticationToken("owner@example.test", null, List.of());
        mvc.perform(delete("/api/posts/post").principal(password)).andExpect(status().isNoContent());
        var oidc = mock(org.springframework.security.oauth2.core.oidc.user.OidcUser.class);
        when(oidc.getEmail()).thenReturn("owner@example.test");
        var google = new UsernamePasswordAuthenticationToken(oidc, null, List.of());
        mvc.perform(delete("/api/posts/post").principal(google)).andExpect(status().isNoContent());
        verify(posts, times(2)).deletePost("post", user.getId().toString());
    }
}
