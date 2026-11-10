package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.repository.*;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.dao.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostCreationRetryTest {
    private final PostRepository posts=mock(PostRepository.class);
    private final LikeRepository likes=mock(LikeRepository.class);
    private final CommentRepository comments=mock(CommentRepository.class);
    private final UserRepository users=mock(UserRepository.class);
    private final MediaStorageService media=mock(MediaStorageService.class);
    private final PostService service=new PostService(posts,likes,comments,users,media,FeedCacheTestSupport.passthrough());
    private final UUID owner=UUID.randomUUID();
    private final String key=UUID.randomUUID().toString();

    @Test void retryAfterLostResponseReturnsOriginalPostAndRejectsChangedContent() {
        var saved=new AtomicReference<Post>();
        when(posts.findById(anyString())).thenAnswer(i->Optional.ofNullable(saved.get()));
        when(posts.insert(any(Post.class))).thenAnswer(i->{saved.set(i.getArgument(0));return saved.get();});
        var first=service.createPostOnce(owner,"text",null,key);
        assertEquals(first.id(),service.createPostOnce(owner,"text",null,key).id());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.createPostOnce(owner,"changed",null,key)).getStatusCode().value());
        verify(posts,times(1)).insert(any(Post.class));
        verify(posts,never()).save(any());
    }

    @Test void ambiguousInsertAcknowledgementIsResolvedByRetryWithoutAnotherPost() {
        var saved=new AtomicReference<Post>();
        when(posts.findById(anyString())).thenAnswer(i->Optional.ofNullable(saved.get()));
        when(posts.insert(any(Post.class))).thenAnswer(i->{saved.set(i.getArgument(0));throw new DataAccessResourceFailureException("acknowledgement lost");});
        assertThrows(DataAccessResourceFailureException.class,()->service.createPostOnce(owner,"text",null,key));
        assertEquals(saved.get().getId(),service.createPostOnce(owner,"text",null,key).id());
        verify(posts,times(1)).insert(any(Post.class));
        verifyNoInteractions(media);
    }

    @Test void concurrentDuplicateInsertReturnsTheWinner() {
        var winner=new AtomicReference<Post>();
        when(posts.findById(anyString())).thenAnswer(i->Optional.ofNullable(winner.get()));
        when(posts.insert(any(Post.class))).thenAnswer(i->{winner.set(i.getArgument(0));throw new DuplicateKeyException("concurrent winner");});
        assertEquals("text",service.createPostOnce(owner,"text",null,key).content());
        verify(posts,times(1)).insert(any(Post.class));
    }

    @Test void malformedKeyDoesNotReachStorage() {
        assertThrows(ResponseStatusException.class,()->service.createPostOnce(owner,"text",null,"invalid"));
        verifyNoInteractions(posts,media);
    }

    @Test void retryCannotRecreateADeletedPost() {
        var saved=new AtomicReference<Post>();
        when(posts.findById(anyString())).thenAnswer(i->Optional.ofNullable(saved.get()));
        when(posts.insert(any(Post.class))).thenAnswer(i->{saved.set(i.getArgument(0));return saved.get();});
        service.createPostOnce(owner,"text",null,key);
        saved.get().setDeletionRequested(true);
        assertEquals(410,assertThrows(ResponseStatusException.class,()->service.createPostOnce(owner,"text",null,key)).getStatusCode().value());
        verify(posts,times(1)).insert(any(Post.class));
    }
}
