package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.dto.response.PostResponse;
import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.repository.CommentRepository;
import com.net.cardo.xcopybackend.post.repository.LikeRepository;
import com.net.cardo.xcopybackend.post.repository.PostRepository;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final MediaStorageService mediaStorageService;
    private final FeedCacheService feedCacheService;




    private PostResponse toResponse(Post post, String currentUserId) {

        User author = findAuthor(post.getUserId());

        long likesCount = likeRepository.countByPostId(post.getId());

        long commentsCount = commentRepository.countByPostId(post.getId());

        boolean likedByMe = likeRepository.existsByPostIdAndUserId(
                post.getId(),
                currentUserId
        );

        return new PostResponse(
                post.getId(),
                post.getUserId(),
                author == null ? "deleted" : author.getUsername(),
                author == null ? "Видалений користувач" : author.getDisplayName(),
                post.getContent(),
                post.getMediaType(),
                post.getMediaUrl(),
                post.getCreatedAt(),
                likesCount,
                commentsCount,
                likedByMe,
                author == null ? null : author.getAvatarUrl()
        );
    }

    private User findAuthor(String userId) {
        if (userId == null) return null;
        UUID authorId;
        try {
            authorId = UUID.fromString(userId);
        } catch (IllegalArgumentException invalidId) {
            return null;
        }
        // MongoDB posts can outlive their PostgreSQL author.
        return userRepository.findById(authorId).orElse(null);
    }
    public PostResponse createPost(
            UUID userId,
            String content,
            org.springframework.web.multipart.MultipartFile file
    ) {
        Post post = new Post();

        post.setUserId(userId.toString());
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            MediaStorageService.StoredMedia media = mediaStorageService.store(file);
            post.setMediaUrl(media.url());
            post.setMediaType(media.contentType());
        }

        Post savedPost = postRepository.save(post);
        feedCacheService.invalidate();

        return toResponse(
                savedPost,
                userId.toString()
        );
    }

    @Cacheable(
            cacheNames = FeedCacheService.CACHE_NAME,
            key = "'all:' + #a0",
            sync = true
    )
    public List<PostResponse> getPosts(String currentUserId) {

        return postRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(post -> toResponse(post, currentUserId))
                .toList();
    }

    @Cacheable(
            cacheNames = FeedCacheService.CACHE_NAME,
            key = "'user:' + #a0 + ':viewer:' + #a1",
            sync = true
    )
    public List<PostResponse> getUserPosts(
            String userId,
            String currentUserId
    ) {
        return postRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(post -> toResponse(post, currentUserId))
                .toList();
    }

    public void deletePost(String id, String currentUserId) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Увійдіть в акаунт знову");
        }
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Допис не знайдено"));
        if (!currentUserId.equals(post.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можна видаляти лише власні дописи");
        }
        postRepository.delete(post);
        mediaStorageService.delete(post.getMediaUrl());
        feedCacheService.invalidate();
    }
}

