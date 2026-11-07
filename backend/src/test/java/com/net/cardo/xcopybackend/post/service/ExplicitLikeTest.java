package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.exception.exceptions.*;

import com.net.cardo.xcopybackend.post.entity.Comment;
import com.net.cardo.xcopybackend.post.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExplicitLikeTest {
    final LikeRepository likes=mock(LikeRepository.class);final PostRepository posts=mock(PostRepository.class);
    final LikeService service=new LikeService(likes,posts);
    @Test void repeatingPutRequestsNeverTogglesTheState() {
        when(posts.existsActiveById("p")).thenReturn(true);when(likes.countByPostId("p")).thenReturn(1L);
        assertTrue(service.setLiked("p","u",true).likedByMe());assertTrue(service.setLiked("p","u",true).likedByMe());
        verify(likes,times(2)).ensureLiked(eq("p"),eq("u"),any());verify(likes,never()).deleteByPostIdAndUserId(anyString(),anyString());
    }
    @Test void repeatingDeleteIsSuccessfulEvenWhenPostAlreadyDisappeared() {
        assertFalse(service.setLiked("p","u",false).likedByMe());assertEquals(0,service.setLiked("p","u",false).likesCount());
        verify(likes,times(2)).deleteByPostIdAndUserId("p","u");verify(likes,never()).ensureLiked(anyString(),anyString(),any());
    }
    @Test void uniquePairRaceSucceedsButAnUnrelatedConstraintFailurePropagates() {
        when(posts.existsActiveById("p")).thenReturn(true);
        doThrow(new DuplicateKeyException("pair")).when(likes).ensureLiked(eq("p"),eq("u"),any());
        when(likes.existsByPostIdAndUserId("p","u")).thenReturn(true);
        assertTrue(service.setLiked("p","u",true).likedByMe());
        when(likes.existsByPostIdAndUserId("p","u")).thenReturn(false);
        assertThrows(DuplicateKeyException.class,()->service.setLiked("p","u",true));
    }
    @Test void deletionBetweenChecksRemovesTheLateLikeAndReturns404() {
        when(posts.existsActiveById("p")).thenReturn(true,false);
        assertThrows(PostNotFoundException.class,()->service.setLiked("p","u",true));
        verify(likes).ensureLiked(eq("p"),eq("u"),any());verify(likes).deleteByPostIdAndUserId("p","u");
    }
    @Test void lateCommentIsCompensatedAfterItsParentIsDeleted() {
        var comments=mock(CommentRepository.class);var saved=new Comment();saved.setId("comment");
        when(posts.existsActiveById("p")).thenReturn(true,false);when(comments.save(any())).thenReturn(saved);
        assertThrows(PostNotFoundException.class,()->new CommentService(comments,posts).createComment("p","u","text"));
        verify(comments).deleteById("comment");
    }
    @Test void commentDeletionDistinguishesMissingAndForbidden() {
        var comments=mock(CommentRepository.class);var service=new CommentService(comments,posts);
        assertThrows(CommentNotFoundException.class,()->service.deleteComment("missing","u","p"));
        var other=new Comment();other.setUserId("other");other.setPostId("p");when(comments.findById("c")).thenReturn(Optional.of(other));
        assertThrows(CommentAccessDeniedException.class,()->service.deleteComment("c","u","p"));
        assertThrows(CommentNotFoundException.class,()->service.deleteComment("c","u","wrong-post"));
    }
}
