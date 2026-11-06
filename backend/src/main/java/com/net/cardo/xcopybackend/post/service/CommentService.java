package com.net.cardo.xcopybackend.post.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    public Comment createComment(
            String postId,
            String userId,
            String content
    ) {

        if (!postRepository.existsActiveById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Допис не знайдено");
        }

        if (content == null || content.isBlank() || content.length()>5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Коментар має містити від 1 до 5000 символів");
        }

        Comment comment = new Comment();

        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        if(!postRepository.existsActiveById(postId)) {
            commentRepository.deleteById(savedComment.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Допис видалено");
        }

        return savedComment;
    }

    public List<Comment> getComments(String postId, int page, int size) {

        if (!postRepository.existsActiveById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Допис не знайдено");
        }

        return commentRepository
                .findByPostIdOrderByCreatedAtDesc(postId,PostService.page(page,size));
    }
    public void deleteComment(
            String commentId,
            String userId,
            String postId
    ) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,"Коментар не знайдено")
                );
        if (!comment.getPostId().equals(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Коментар не знайдено");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Можна видаляти лише власні коментарі");
        }

        commentRepository.delete(comment);

    }

}
