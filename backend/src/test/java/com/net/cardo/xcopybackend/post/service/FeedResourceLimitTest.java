package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.repository.*;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedResourceLimitTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final LikeRepository likes = mock(LikeRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MediaStorageService media = mock(MediaStorageService.class);
    private final FeedCacheService cache = mock(FeedCacheService.class);
    private final PostService service = new PostService(posts,likes,comments,users,media,cache);
    @Test void everyFeedQueriesOnlyTheRequestedPage() {
        service.getPosts("viewer",2,20);
        service.getUserPosts("author","viewer",3,20);
        service.getPostsByAuthors(List.of("author"),"viewer",4,20);
        var page = ArgumentCaptor.forClass(Pageable.class);
        verify(posts).findAllByOrderByCreatedAtDesc(page.capture());
        verify(posts).findByUserIdOrderByCreatedAtDesc(eq("author"),page.capture());
        verify(posts).findByUserIdInOrderByCreatedAtDesc(eq(List.of("author")),page.capture());
        assertEquals(List.of(2,3,4),page.getAllValues().stream().map(Pageable::getPageNumber).toList());
        assertTrue(page.getAllValues().stream().allMatch(value->value.getPageSize()==20));
    }
    @Test void oversizedPageRequestsCannotReachTheDatabaseEvenForAnEmptyFollowingFeed() {
        assertThrows(ResponseStatusException.class,()->service.getPosts("viewer",0,100_000));
        assertThrows(ResponseStatusException.class,()->service.getUserPosts("author","viewer",0,100_000));
        assertThrows(ResponseStatusException.class,()->service.getPostsByAuthors(List.of(),"viewer",0,100_000));
        verifyNoInteractions(posts,users,likes,comments);
    }
    @Test void oversizedPostTextIsRejectedBeforeStorageAndDatabaseWrites() {
        assertThrows(ResponseStatusException.class,()->service.createPost(UUID.randomUUID(),"x".repeat(5001),null));
        verifyNoInteractions(posts,users,media,cache);
    }
}
