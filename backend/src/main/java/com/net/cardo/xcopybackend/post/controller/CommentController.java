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

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> createComment(
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

        return ResponseEntity.ok(comment);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Comment>> getComments(
            @PathVariable String postId
    ) {

        return ResponseEntity.ok(
                commentService.getComments(postId)
        );
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
}
