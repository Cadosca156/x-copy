package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LikeServiceConcurrencyTest {
    private final LikeRepository likes = mock(LikeRepository.class);
    private final PostRepository posts = mock(PostRepository.class);
    private final LikeService service = new LikeService(likes,posts);
    @Test void concurrentAddThatLostTheUniqueIndexRaceStillReturnsLiked() {
        when(posts.existsById("post")).thenReturn(true);
        when(likes.existsByPostIdAndUserId("post","viewer")).thenReturn(false,true);
        when(likes.save(any())).thenThrow(new DuplicateKeyException("pair already inserted"));
        assertTrue(service.toggleLike("post","viewer"));
        verify(likes,never()).deleteByPostIdAndUserId(anyString(),anyString());
    }
    @Test void unrelatedDuplicateKeyFailureIsNotSwallowed() {
        when(posts.existsById("post")).thenReturn(true);
        when(likes.existsByPostIdAndUserId("post","viewer")).thenReturn(false);
        when(likes.save(any())).thenThrow(new DuplicateKeyException("different constraint"));
        assertThrows(DuplicateKeyException.class,()->service.toggleLike("post","viewer"));

    }
    @Test void existingLikeIsRemovedNormally() {
        when(posts.existsById("post")).thenReturn(true);
        when(likes.existsByPostIdAndUserId("post","viewer")).thenReturn(true);
        assertFalse(service.toggleLike("post","viewer"));
        verify(likes).deleteByPostIdAndUserId("post","viewer");verify(likes,never()).save(any());

    }
}
