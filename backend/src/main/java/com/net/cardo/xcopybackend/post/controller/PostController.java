package com.net.cardo.xcopybackend.post.controller;


import com.net.cardo.xcopybackend.post.dto.response.PostResponse;
import com.net.cardo.xcopybackend.post.service.LikeService;
import com.net.cardo.xcopybackend.post.service.PostService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;
    private final LikeService likeService;



    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @RequestParam(value = "content", defaultValue = "") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication
    ) {
        boolean hasFile = file != null && !file.isEmpty();

        if (content.isBlank() && !hasFile) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Додай текст, фото або відео"
            );
        }

        String email;

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
        } else {
            email = authentication.getName();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Користувача не знайдено"
                ));

        PostResponse post = postService.createPost(
                user.getId(),
                content.trim(),
                file
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getPosts(
            Authentication authentication
    ) {

        String email;

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
        } else {
            email = authentication.getName();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Увійдіть в акаунт знову")
                );

        List<PostResponse> posts =
                postService.getPosts(
                        user.getId().toString()
                );

        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getUserPosts(
            @PathVariable String userId,
            Authentication authentication
    ) {
        String email;

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
        } else {
            email = authentication.getName();
        }

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Увійдіть в акаунт знову"));

        return ResponseEntity.ok(
                postService.getUserPosts(
                        userId,
                        currentUser.getId().toString()
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable String id,
            Authentication authentication
    ) {

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Увійдіть в акаунт знову");
        }
        String email = authentication.getPrincipal() instanceof OidcUser oidcUser
                ? oidcUser.getEmail() : authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Увійдіть в акаунт знову"));
        postService.deletePost(id, user.getId().toString());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Boolean> toggleLike(
            @PathVariable String postId,
            Authentication authentication
    ) {

        String email;

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
        } else {
            email = authentication.getName();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Увійдіть в акаунт знову")
                );

        boolean liked = likeService.toggleLike(
                postId,
                user.getId().toString()
        );

        return ResponseEntity.ok(liked);
    }
}

