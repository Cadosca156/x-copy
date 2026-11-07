package com.net.cardo.xcopybackend.post.controller;

import com.net.cardo.xcopybackend.exception.exceptions.AuthenticationRequiredException;
import com.net.cardo.xcopybackend.exception.exceptions.InvalidPaginationException;
import com.net.cardo.xcopybackend.post.dto.response.LikeState;
import com.net.cardo.xcopybackend.post.dto.response.PostResponse;
import com.net.cardo.xcopybackend.post.dto.response.PostWindow;
import com.net.cardo.xcopybackend.post.service.LikeService;
import com.net.cardo.xcopybackend.post.service.PostService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;
    private final LikeService likeService;

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationRequiredException();
        }
        String email = authentication.getPrincipal() instanceof OidcUser oidc ? oidc.getEmail() : authentication.getName();
        return com.net.cardo.xcopybackend.security.SessionAccounts.find(userRepository, email).orElseThrow(AuthenticationRequiredException::new);
    }

    @GetMapping(params = "pagination=cursor")
    public PostWindow postWindow(@RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer page, Authentication authentication) {
        rejectOffset(page);
        return postService.getPostWindow(authenticatedUser(authentication).getId().toString(), cursor, size);
    }

    @GetMapping(value = "/user/{userId}", params = "pagination=cursor")
    public PostWindow userPostWindow(@PathVariable String userId, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) Integer page, Authentication authentication) {
        rejectOffset(page);
        return postService.getUserPostWindow(userId, authenticatedUser(authentication).getId().toString(), cursor, size);
    }

    public static void rejectOffset(Integer page) {
        if (page != null) throw new InvalidPaginationException("Не поєднуйте номер сторінки з курсором");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(@RequestParam(value = "content", defaultValue = "") String content,
            @RequestParam(value = "file", required = false) MultipartFile file, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(authenticatedUser(authentication).getId(), content, file));
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getPosts(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return ResponseEntity.ok(postService.getPosts(authenticatedUser(authentication).getId().toString(), page, size));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getUserPosts(@PathVariable String userId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return ResponseEntity.ok(postService.getUserPosts(userId, authenticatedUser(authentication).getId().toString(), page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable String id, Authentication authentication) {
        postService.deletePost(id, authenticatedUser(authentication).getId().toString());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{postId}/like")
    public LikeState like(@PathVariable String postId, Authentication authentication) {
        return likeService.setLiked(postId, authenticatedUser(authentication).getId().toString(), true);
    }

    @DeleteMapping("/{postId}/like")
    public LikeState unlike(@PathVariable String postId, Authentication authentication) {
        return likeService.setLiked(postId, authenticatedUser(authentication).getId().toString(), false);
    }

    @Deprecated
    @PostMapping("/{postId}/like")
    public ResponseEntity<Boolean> toggleLike(@PathVariable String postId, Authentication authentication) {
        return ResponseEntity.ok(likeService.toggleLike(postId, authenticatedUser(authentication).getId().toString()));
    }
}
