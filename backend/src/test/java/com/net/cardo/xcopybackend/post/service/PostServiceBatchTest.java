package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.repository.*;
import com.net.cardo.xcopybackend.post.repository.projection.*;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostServiceBatchTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final LikeRepository likes = mock(LikeRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PostService service = new PostService(posts,likes,comments,users,
            mock(MediaStorageService.class),mock(FeedCacheService.class));

    private Post post(String id, String author) {
        var post = new Post();post.setId(id);post.setUserId(author);return post;
    }
    @Test void queryBudgetStaysFiveForOneTwentyAndFiftyPosts() {
        for (int size : List.of(1,20,50)) {
            reset(posts,likes,comments,users);
            UUID authorId = UUID.randomUUID();
            List<Post> page = IntStream.range(0,size).mapToObj(i->post("post-"+i,authorId.toString())).toList();
            when(posts.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);
            var result = service.getPosts("viewer",0,size);
            assertEquals(page.stream().map(Post::getId).toList(),result.stream().map(p->p.id()).toList());
            List<String> ids = page.stream().map(Post::getId).toList();
            verify(posts).findAllByOrderByCreatedAtDesc(any());
            verify(users).findAllById(List.of(authorId));
            verify(likes).countForPosts(ids);verify(comments).countForPosts(ids);
            verify(likes).findLikedPostIds(ids,"viewer");
            // Fails if a per-post count/exists/findById call is reintroduced.
            verifyNoMoreInteractions(posts,users,likes,comments);
        }
    }
    @Test void mapsUnorderedAggregationsAndAuthorsWithoutReorderingThePage() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        var alice = new User();alice.setId(a);alice.setDisplayName("Alice");
        var bob = new User();bob.setId(b);bob.setDisplayName("Bob");
        when(posts.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of(post("new",a.toString()),post("old",b.toString())));
        when(users.findAllById(List.of(a,b))).thenReturn(List.of(bob,alice));
        when(likes.countForPosts(anyList())).thenReturn(List.of(new PostCount("old",7),new PostCount("new",2)));
        when(comments.countForPosts(anyList())).thenReturn(List.of(new PostCount("old",4)));
        when(likes.findLikedPostIds(anyList(),eq("viewer"))).thenReturn(List.of(new LikedPostId("old")));
        var result = service.getPosts("viewer");
        assertEquals(List.of("new","old"),result.stream().map(p->p.id()).toList());
        assertEquals("Alice",result.get(0).displayName());assertEquals("Bob",result.get(1).displayName());
        assertEquals(2,result.get(0).likesCount());assertEquals(0,result.get(0).commentsCount());
        assertFalse(result.get(0).likedByMe());assertEquals(7,result.get(1).likesCount());
        assertEquals(4,result.get(1).commentsCount());assertTrue(result.get(1).likedByMe());
    }
    @Test void emptyPageDoesNotLoadAnyRelatedData() {
        when(posts.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of());
        assertEquals(List.of(),service.getPosts("viewer"));
        verifyNoInteractions(users,likes,comments);
    }
    @Test void profileAndFollowingUseTheSameBatchLoader() {
        UUID author = UUID.randomUUID();var page = List.of(post("post",author.toString()));
        when(posts.findByUserIdOrderByCreatedAtDesc(eq(author.toString()),any())).thenReturn(page);
        when(posts.findByUserIdInOrderByCreatedAtDesc(eq(List.of(author.toString())),any())).thenReturn(page);
        assertEquals(1,service.getUserPosts(author.toString(),"viewer").size());
        assertEquals(1,service.getPostsByAuthors(List.of(author.toString()),"viewer").size());
        verify(users,times(2)).findAllById(List.of(author));
        verify(likes,times(2)).countForPosts(List.of("post"));
        verify(comments,times(2)).countForPosts(List.of("post"));
        verify(likes,times(2)).findLikedPostIds(List.of("post"),"viewer");
        verifyNoMoreInteractions(users,likes,comments);
    }
    @Test void missingCountsMeanZeroButDatabaseFailureIsNotHidden() {
        var page = List.of(post("post",null));
        when(posts.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);
        var result = service.getPosts("viewer").get(0);
        assertEquals(0,result.likesCount());assertEquals(0,result.commentsCount());assertFalse(result.likedByMe());
        when(likes.countForPosts(anyList())).thenThrow(new DataAccessResourceFailureException("offline"));
        assertThrows(DataAccessResourceFailureException.class,()->service.getPosts("viewer"));
    }
}
