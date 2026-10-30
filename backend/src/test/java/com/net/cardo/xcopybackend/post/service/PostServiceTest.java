package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.repository.*;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostServiceTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final LikeRepository likes = mock(LikeRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PostService service = new PostService(posts, likes, comments, users,
            mock(MediaStorageService.class), mock(FeedCacheService.class));

    private Post post(String id, String author) {
        var post = new Post();
        post.setId(id);
        post.setUserId(author);
        post.setContent("Original content");
        post.setMediaUrl("/media/posts/image.png");
        return post;
    }

    @Test
    void deletedAuthorDoesNotBreakOtherPostsInTheFeed() {
        UUID activeId = UUID.randomUUID();
        UUID deletedId = UUID.randomUUID();
        var author = new User();
        author.setUsername("active");
        author.setDisplayName("Active User");
        author.setAvatarUrl("/media/posts/avatar.png");
        when(users.findById(activeId)).thenReturn(Optional.of(author));
        when(posts.findAllByOrderByCreatedAtDesc(any(org.springframework.data.domain.Pageable.class))).thenReturn(List.of(
                post("old-post", deletedId.toString()), post("active-post", activeId.toString())));
        when(likes.countByPostId("old-post")).thenReturn(3L);
        when(comments.countByPostId("old-post")).thenReturn(2L);
        when(likes.existsByPostIdAndUserId("old-post", "viewer")).thenReturn(true);

        var feed = service.getPosts("viewer");
        assertEquals(2, feed.size());
        var old = feed.get(0);
        assertEquals("Видалений користувач", old.displayName());
        assertEquals("deleted", old.username());
        assertNull(old.avatarUrl());
        assertEquals(deletedId.toString(), old.userId());
        assertEquals("Original content", old.content());
        assertEquals("/media/posts/image.png", old.mediaUrl());
        assertEquals(3, old.likesCount());
        assertEquals(2, old.commentsCount());
        assertTrue(old.likedByMe());
        assertEquals("Active User", feed.get(1).displayName());
        assertEquals("/media/posts/avatar.png", feed.get(1).avatarUrl());
        verify(posts, never()).delete(any());
    }

    @Test
    void deletedAuthorsPostsCanStillBeListedByUser() {
        String deletedId = UUID.randomUUID().toString();
        when(posts.findByUserIdOrderByCreatedAtDesc(eq(deletedId), any(org.springframework.data.domain.Pageable.class))).thenReturn(List.of(post("old-post", deletedId)));
        assertEquals("Видалений користувач", service.getUserPosts(deletedId, "viewer").get(0).displayName());
    }

    @Test
    void malformedOrMissingLegacyAuthorIdsDoNotBreakTheFeed() {
        when(posts.findAllByOrderByCreatedAtDesc(any(org.springframework.data.domain.Pageable.class))).thenReturn(List.of(post("bad", "invalid-id"), post("null", null)));
        assertEquals(2, service.getPosts("viewer").size());
        verifyNoInteractions(users);
    }

    @Test
    void databaseOutageIsNotMistakenForADeletedAuthor() {
        UUID id = UUID.randomUUID();
        when(posts.findAllByOrderByCreatedAtDesc(any(org.springframework.data.domain.Pageable.class))).thenReturn(List.of(post("post", id.toString())));
        when(users.findById(id)).thenThrow(new DataAccessResourceFailureException("Database unavailable"));
        assertThrows(DataAccessResourceFailureException.class, () -> service.getPosts("viewer"));
    }

    @Test
    void followingFeedQueriesOnlySelectedAuthorsInNewestFirstOrder() {
        String authorId = UUID.randomUUID().toString();
        when(posts.findByUserIdInOrderByCreatedAtDesc(eq(List.of(authorId)), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(post("newest", authorId), post("older", authorId)));
        var feed = service.getPostsByAuthors(List.of(authorId), "viewer");
        assertEquals(List.of("newest", "older"), feed.stream().map(p -> p.id()).toList());
        verify(posts, never()).findAllByOrderByCreatedAtDesc(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void followingNobodyReturnsEmptyFeedWithoutQueryingAllPosts() {
        assertEquals(List.of(), service.getPostsByAuthors(List.of(), "viewer"));
        verifyNoInteractions(posts);
    }
}
