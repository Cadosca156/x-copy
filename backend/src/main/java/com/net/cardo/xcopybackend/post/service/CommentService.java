package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Comment;
import com.net.cardo.xcopybackend.post.repository.CommentRepository;
import com.net.cardo.xcopybackend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final FeedCacheService feedCacheService;

    public Comment createComment(
            String postId,
            String userId,
            String content
    ) {

        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }

        if (content == null || content.isBlank()) {
            throw new RuntimeException("Comment cannot be empty");
        }

        Comment comment = new Comment();

        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        feedCacheService.invalidate();
        return savedComment;
    }

    public List<Comment> getComments(String postId) {

        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }

        return commentRepository
                .findByPostIdOrderByCreatedAtDesc(postId);
    }
    public void deleteComment(
            String commentId,
            String userId,
            String postId
    ) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new RuntimeException("Comment not found")
                );
        if (!comment.getPostId().equals(postId)) {
            throw new RuntimeException(
                    "Comment does not belong to this post"
            );
        }
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException(
                    "You cannot delete another user's comment"
            );
        }

        commentRepository.delete(comment);
        feedCacheService.invalidate();
    }

}

