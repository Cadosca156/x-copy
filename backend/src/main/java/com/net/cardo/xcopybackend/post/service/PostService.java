package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.dto.response.PostResponse;
import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.repository.CommentRepository;
import com.net.cardo.xcopybackend.post.repository.LikeRepository;
import com.net.cardo.xcopybackend.post.repository.PostRepository;
import com.net.cardo.xcopybackend.post.repository.projection.PostCount;
import com.net.cardo.xcopybackend.post.repository.projection.LikedPostId;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final MediaStorageService mediaStorageService;
    private final FeedCacheService feedCacheService;

    /** Load related data once for the bounded page, then assemble DTOs without I/O. */
    private List<PostResponse> toResponses(List<Post> posts, String viewerId) {
        if (posts.isEmpty()) return List.of();

        List<String> postIds = posts.stream().map(Post::getId).distinct().toList();
        List<UUID> authorIds = posts.stream().map(Post::getUserId)
                .map(PostService::parseAuthorId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, User> authors = authorIds.isEmpty() ? Map.of()
                : userRepository.findAllById(authorIds).stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
        Map<String, Long> likes = countsByPost(likeRepository.countForPosts(postIds));
        Map<String, Long> comments = countsByPost(commentRepository.countForPosts(postIds));
        Set<String> likedByViewer = likeRepository.findLikedPostIds(postIds, viewerId).stream()
                .map(LikedPostId::postId).collect(Collectors.toSet());

        // Iterate the original page: aggregation/findAllById result ordering is irrelevant.
        return posts.stream().map(post -> {
            UUID authorId = parseAuthorId(post.getUserId());
            User author = authorId == null ? null : authors.get(authorId);
            return toResponse(post, author, likes.getOrDefault(post.getId(), 0L),
                    comments.getOrDefault(post.getId(), 0L), likedByViewer.contains(post.getId()));
        }).toList();
    }

    private static Map<String, Long> countsByPost(List<PostCount> counts) {
        return counts.stream().collect(Collectors.toMap(PostCount::postId, PostCount::count));
    }

    private static UUID parseAuthorId(String userId) {
        if (userId == null) return null;
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException invalidId) {
            // Legacy MongoDB posts can have a malformed ID or outlive their SQL author.
            return null;
        }
    }

    /** Pure mapping: no repository calls inside the per-post loop. */
    private static PostResponse toResponse(Post post, User author, long likes, long comments, boolean likedByMe) {
        return new PostResponse(post.getId(), post.getUserId(),
                author == null ? "deleted" : author.getUsername(),
                author == null ? "Видалений користувач" : author.getDisplayName(),
                post.getContent(), post.getMediaType(), post.getMediaUrl(), post.getCreatedAt(),
                likes, comments, likedByMe, author == null ? null : author.getAvatarUrl());
    }

    public PostResponse createPost(UUID userId, String content, MultipartFile file) {
        if (content != null && content.length() > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Текст допису має містити до 5000 символів");
        }
        Post post = new Post();
        post.setUserId(userId.toString());
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());
        if (file != null && !file.isEmpty()) {
            MediaStorageService.StoredMedia media = mediaStorageService.store(file, userId);
            post.setMediaUrl(media.url());
            post.setMediaType(media.contentType());
        }
        Post savedPost;
        try { savedPost = postRepository.save(post); }
        catch (RuntimeException failure) {
            mediaStorageService.delete(post.getMediaUrl());
            throw failure;
        }
        feedCacheService.invalidate(userId.toString());
        return toResponses(List.of(savedPost), userId.toString()).get(0);
    }

    public List<PostResponse> getPosts(String currentUserId, int page, int size) {
        var pageable = page(page, size);
        return toResponses(feedCacheService.load("all",page,size,
                () -> postRepository.findAllByOrderByCreatedAtDesc(pageable)), currentUserId);
    }

    public List<PostResponse> getUserPosts(String userId, String currentUserId, int page, int size) {
        var pageable = page(page, size);
        return toResponses(feedCacheService.load("author:"+userId,page,size,
                () -> postRepository.findByUserIdOrderByCreatedAtDesc(userId,pageable)), currentUserId);
    }

    public List<PostResponse> getPostsByAuthors(List<String> authorIds, String viewerId, int page, int size) {
        var pageable = page(page, size);
        if (authorIds.isEmpty()) return List.of();
        return toResponses(postRepository.findByUserIdInOrderByCreatedAtDesc(authorIds, pageable), viewerId);
    }

    public static Pageable page(int page, int size) {
        if (page < 0 || page > 1000 || size < 1 || size > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неприпустимий розмір сторінки");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }
    public List<PostResponse> getPosts(String viewer) { return getPosts(viewer, 0, 20); }
    public List<PostResponse> getUserPosts(String author, String viewer) { return getUserPosts(author, viewer, 0, 20); }
    public List<PostResponse> getPostsByAuthors(List<String> authors, String viewer) { return getPostsByAuthors(authors, viewer, 0, 20); }

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
        feedCacheService.invalidate(post.getUserId());
    }
}
