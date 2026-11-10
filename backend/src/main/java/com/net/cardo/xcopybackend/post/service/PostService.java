package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.dto.response.PostResponse;
import com.net.cardo.xcopybackend.post.dto.response.PostWindow;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.net.cardo.xcopybackend.exception.exceptions.*;

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
        content=PostRules.content(content,file);
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

    /** MongoDB's unique _id makes retries safe across backend processes and Redis outages. */
    public PostResponse createPostOnce(UUID userId, String content, MultipartFile file, String key) {
        if(key==null || !key.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid Idempotency-Key");
        content=PostRules.content(content,file);
        String id=com.net.cardo.xcopybackend.security.EphemeralAuthStore.digest(userId+":"+key.toLowerCase(java.util.Locale.ROOT));
        String fingerprint=fingerprint(content,file);
        var existing=postRepository.findById(id);
        if(existing.isPresent())return replay(existing.get(),userId,fingerprint);
        Post post=new Post();post.setId(id);post.setUserId(userId.toString());post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());post.setCreationFingerprint(fingerprint);
        if(file!=null && !file.isEmpty()) {
            var media=mediaStorageService.store(file,userId);
            post.setMediaUrl(media.url());post.setMediaType(media.contentType());
        }
        Post saved;
        try { saved=postRepository.insert(post); }
        catch(org.springframework.dao.DuplicateKeyException concurrentRetry) {
            // Each attempt stages a different file. Only the winning document retains its file.
            mediaStorageService.delete(post.getMediaUrl());
            return replay(postRepository.findById(id).orElseThrow(() -> concurrentRetry),userId,fingerprint);
        }
        // Other insert errors can be ambiguous: do not delete media that MongoDB may have committed.
        // Repeating with the same key resolves the outcome using the durable document.
        feedCacheService.invalidate(userId.toString());
        return toResponses(List.of(saved),userId.toString()).get(0);
    }

    private PostResponse replay(Post post,UUID userId,String fingerprint) {
        if(!userId.toString().equals(post.getUserId()) || !fingerprint.equals(post.getCreationFingerprint()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Idempotency-Key already used for another post");
        if(post.isDeletionRequested())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE,"Post was deleted");
        return toResponses(List.of(post),userId.toString()).get(0);
    }

    private String fingerprint(String content,MultipartFile file) {
        try {
            var digest=java.security.MessageDigest.getInstance("SHA-256");
            digest.update(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));digest.update((byte)0);
            if(file!=null && !file.isEmpty()) {
                if(file.getSize()>25L*1024*1024)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
                digest.update(String.valueOf(file.getContentType()).getBytes(java.nio.charset.StandardCharsets.UTF_8));digest.update((byte)0);
                try(var input=file.getInputStream()) {
                    byte[] buffer=new byte[8192];int count;long read=0;
                    while((count=input.read(buffer))!=-1) {
                        read+=count;
                        if(read>25L*1024*1024 || read>file.getSize())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
                        digest.update(buffer,0,count);
                    }
                    if(read!=file.getSize())throw new java.io.IOException("Invalid upload length");
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch(java.io.IOException failure) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Cannot read upload",failure);
        } catch(java.security.NoSuchAlgorithmException impossible) {throw new IllegalStateException(impossible);}
    }

    public List<PostResponse> getPosts(String currentUserId, int page, int size) {
        var pageable = page(page, size);
        return toResponses(feedCacheService.load("all",page,size,
                () -> postRepository.findAllByOrderByCreatedAtDesc(pageable)), currentUserId);
    }

    public PostWindow getPostWindow(String viewer,String cursor,int size) {
        return window("all",null,null,viewer,cursor,size,true);
    }
    public PostWindow getUserPostWindow(String author,String viewer,String cursor,int size) {
        try { author=UUID.fromString(author).toString(); }
        catch(IllegalArgumentException invalid) {throw new InvalidAuthorException("Неправильний ID автора");}
        return window("author:"+author,author,null,viewer,cursor,size,true);
    }
    public PostWindow getFollowingPostWindow(List<String> authors,String viewer,String cursor,int size) {
        return window("following:"+viewer,null,authors,viewer,cursor,size,false);
    }
    private PostWindow window(String scope,String author,List<String> authors,String viewer,String cursor,int size,boolean cacheStart) {
        if(size<1 || size>50)throw new InvalidPaginationException("Неприпустимий розмір сторінки");
        var before=FeedCursor.decode(scope,cursor);
        if(authors!=null && authors.isEmpty())return new PostWindow(List.of(),null,false);
        java.util.function.Supplier<List<Post>> loader=()->postRepository.findWindow(author,authors,before,size+1);
        // Only the common first window is cached; arbitrary positions do not grow Redis keyspace.
        List<Post> found=before==null && cacheStart?feedCacheService.load(scope,"cursor:start",size+1,loader):loader.get();
        boolean more=found.size()>size;
        List<Post> visible=found.subList(0,Math.min(size,found.size()));
        String next=null;
        if(more) {
            Post last=visible.get(visible.size()-1);
            next=FeedCursor.encode(scope,new FeedCursor.Position(last.getCreatedAt(),last.getId()));
        }
        return new PostWindow(toResponses(visible,viewer),next,more);
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
            throw new InvalidPaginationException("Неприпустимий розмір сторінки");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }
    public List<PostResponse> getPosts(String viewer) { return getPosts(viewer, 0, 20); }
    public List<PostResponse> getUserPosts(String author, String viewer) { return getUserPosts(author, viewer, 0, 20); }
    public List<PostResponse> getPostsByAuthors(List<String> authors, String viewer) { return getPostsByAuthors(authors, viewer, 0, 20); }

    public void deletePost(String id, String currentUserId) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new AuthenticationRequiredException();
        }
        Post post = postRepository.findById(id).orElse(null);
        if(post==null)return; // Repeating a completed DELETE has the same result.
        if (!currentUserId.equals(post.getUserId())) {
            throw new PostAccessDeniedException("Можна видаляти лише власні дописи");
        }
        postRepository.requestDeletion(id,currentUserId,java.time.Instant.now());
        feedCacheService.invalidate(post.getUserId());
    }
}
