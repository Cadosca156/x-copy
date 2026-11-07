package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.exception.exceptions.*;

import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.repository.*;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostWindowServiceTest {
    final PostRepository posts=mock(PostRepository.class);
    final LikeRepository likes=mock(LikeRepository.class);
    final CommentRepository comments=mock(CommentRepository.class);
    final UserRepository users=mock(UserRepository.class);
    final FeedCacheService cache=FeedCacheTestSupport.passthrough();
    final PostService service=new PostService(posts,likes,comments,users,mock(MediaStorageService.class),cache);
    Post post(String id){var p=new Post();p.setId(id);p.setCreatedAt(LocalDateTime.of(2026,11,6,12,0));p.setUserId(UUID.randomUUID().toString());return p;}
    @Test void lookaheadIsNotReturnedOrEnrichedAndCursorUsesLastVisiblePost() {
        var first=post("c");var second=post("b");var extra=post("a");
        when(posts.findWindow(null,null,null,3)).thenReturn(List.of(first,second,extra));
        var result=service.getPostWindow("viewer",null,2);
        assertEquals(List.of("c","b"),result.items().stream().map(p->p.id()).toList());assertTrue(result.hasMore());
        assertEquals(new FeedCursor.Position(second.getCreatedAt(),"b"),FeedCursor.decode("all",result.nextCursor()));
        verify(likes).countForPosts(List.of("c","b"));verify(comments).countForPosts(List.of("c","b"));
    }
    @Test void continuationUsesPositionWithoutLoadingAnchorOrCachingArbitraryTokens() {
        var anchor=new FeedCursor.Position(LocalDateTime.of(2026,11,6,12,0),"b");
        when(posts.findWindow(null,null,anchor,3)).thenReturn(List.of(post("a")));
        var result=service.getPostWindow("viewer",FeedCursor.encode("all",anchor),2);
        assertFalse(result.hasMore());assertNull(result.nextCursor());assertEquals(1,result.items().size());
        verify(posts,never()).findById(anyString());verifyNoInteractions(cache);
    }
    @Test void invalidCursorAndSizeAreRejectedBeforeLoadingPagesOrMetadata() {
        assertThrows(InvalidCursorException.class,()->service.getPostWindow("v","bad",20));
        assertThrows(InvalidPaginationException.class,()->service.getPostWindow("v",null,51));
        assertThrows(InvalidPaginationException.class,()->service.getPostWindow("v",null,0));
        verifyNoInteractions(posts,likes,comments,users,cache);
    }
    @Test void authorAndFollowingScopesCannotBeSwappedAndEmptyFollowingDoesNoReads() {
        String author=UUID.randomUUID().toString();
        when(posts.findWindow(author,null,null,21)).thenReturn(List.of());
        assertFalse(service.getUserPostWindow(author,"viewer",null,20).hasMore());
        verify(posts).findWindow(author,null,null,21);clearInvocations(posts,cache);
        assertEquals(List.of(),service.getFollowingPostWindow(List.of(),"viewer",null,20).items());
        var wrong=FeedCursor.encode("all",new FeedCursor.Position(LocalDateTime.now(),"id"));
        assertThrows(InvalidCursorException.class,()->service.getFollowingPostWindow(List.of(),"viewer",wrong,20));
        verifyNoInteractions(posts,cache);
    }
}
