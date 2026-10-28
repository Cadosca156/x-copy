package com.net.cardo.xcopybackend.post.controller;

import com.net.cardo.xcopybackend.post.dto.request.CreateCommentRequest;
import com.net.cardo.xcopybackend.post.entity.Comment;
import com.net.cardo.xcopybackend.post.service.CommentService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.net.cardo.xcopybackend.post.dto.response.CommentResponse;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable String postId,
            @RequestBody CreateCommentRequest request,
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
                        new RuntimeException("User not found")
                );

        Comment comment = commentService.createComment(
                postId,
                user.getId().toString(),
                request.content()
        );

        return ResponseEntity.ok(toResponse(comment, user));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable String postId
    ) {

        var comments = commentService.getComments(postId);
        var ids = comments.stream().map(c -> parseUserId(c.getUserId()))
                .filter(Objects::nonNull).distinct().toList();
        var authors = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return ResponseEntity.ok(comments.stream()
                .map(c -> toResponse(c, authors.get(parseUserId(c.getUserId())))).toList());
    }
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId,
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
                        new RuntimeException("User not found")
                );

        commentService.deleteComment(
                commentId,
                user.getId().toString(),
                postId
        );

        return ResponseEntity.noContent().build();
    }

    private static UUID parseUserId(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static CommentResponse toResponse(Comment comment, User author) {
        return new CommentResponse(comment.getId(), comment.getPostId(), comment.getUserId(),
                author == null ? "deleted" : author.getUsername(),
                author == null ? "Видалений користувач" : author.getDisplayName(),
                author == null ? null : author.getAvatarUrl(),
                comment.getContent(), comment.getCreatedAt());
    }
}
