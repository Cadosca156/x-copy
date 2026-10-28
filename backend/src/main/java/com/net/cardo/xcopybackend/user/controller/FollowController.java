package com.net.cardo.xcopybackend.user.controller;

import com.net.cardo.xcopybackend.user.service.FollowService;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import com.net.cardo.xcopybackend.post.service.PostService;
import com.net.cardo.xcopybackend.post.dto.response.PostResponse;
import com.net.cardo.xcopybackend.user.dto.response.SuggestedUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService follows;
    private final UserRepository users;
    private final PostService posts;

    private UUID viewerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Увійдіть в акаунт знову");
        }
        String email = authentication.getPrincipal() instanceof OidcUser oidc
                ? oidc.getEmail() : authentication.getName();
        return users.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Увійдіть в акаунт знову")).getId();
    }

    @GetMapping("/suggestions")
    public List<SuggestedUserResponse> suggestions(Authentication authentication) {
        return follows.suggestions(viewerId(authentication));
    }

    @GetMapping("/me/following")
    public List<UUID> following(Authentication authentication) {
        return follows.followingIds(viewerId(authentication));
    }

    @GetMapping("/me/following/posts")
    public List<PostResponse> followingPosts(Authentication authentication) {
        UUID viewer = viewerId(authentication);
        return posts.getPostsByAuthors(follows.followingIds(viewer).stream().map(UUID::toString).toList(),
                viewer.toString());
    }

    @PutMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable UUID userId, Authentication authentication) {
        follows.follow(viewerId(authentication), userId);
    }

    @DeleteMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable UUID userId, Authentication authentication) {
        follows.unfollow(viewerId(authentication), userId);
    }
}
