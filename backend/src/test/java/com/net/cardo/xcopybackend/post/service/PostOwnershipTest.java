package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.repository.*;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostOwnershipTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final MediaStorageService media = mock(MediaStorageService.class);
    private final FeedCacheService cache = FeedCacheTestSupport.passthrough();
    private final PostService service = new PostService(posts, mock(LikeRepository.class),
            mock(CommentRepository.class), mock(UserRepository.class), media, cache);

    private Post ownedPost(String owner) {
        var post = new Post();
        post.setId("post");
        post.setUserId(owner);
        post.setMediaUrl("/photo.png");
        when(posts.findById("post")).thenReturn(Optional.of(post));
        return post;
    }

    @Test
    void anotherUserCannotDeletePostOrItsMedia() {
        var post = ownedPost("owner");
        var error = assertThrows(ResponseStatusException.class, () -> service.deletePost("post", "attacker"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(posts, never()).delete(post);
        verifyNoInteractions(media, cache);
    }

    @Test
    void ownerCanDeleteTheirPost() {
        var post = ownedPost("owner");
        service.deletePost("post", "owner");
        verify(posts).requestDeletion(eq("post"),eq("owner"),any(java.time.Instant.class));
        verifyNoInteractions(media);
        verify(cache).invalidate("owner");
    }

    @Test
    void missingPostDeleteIsAnIdempotentSuccess() {
        when(posts.findById("missing")).thenReturn(Optional.empty());
        service.deletePost("missing", "owner");
        verify(posts, never()).delete(any());
        verifyNoInteractions(media, cache);
    }

    @Test
    void unauthenticatedCallerCannotDelete() {
        for (String userId : new String[] {null, "", " "}) {
            var error = assertThrows(ResponseStatusException.class, () -> service.deletePost("post", userId));
            assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        }
        verifyNoInteractions(posts, media, cache);
    }

    @Test
    void postWithoutAuthorCannotBeDeletedByAnotherUser() {
        ownedPost(null);
        var error = assertThrows(ResponseStatusException.class, () -> service.deletePost("post", "attacker"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(posts, never()).delete(any());
        verifyNoInteractions(media, cache);
    }
}
