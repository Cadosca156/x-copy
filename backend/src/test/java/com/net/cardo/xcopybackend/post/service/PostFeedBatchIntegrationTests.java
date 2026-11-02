package com.net.cardo.xcopybackend.post.service;

import com.mongodb.event.*;
import com.net.cardo.xcopybackend.post.entity.*;
import com.net.cardo.xcopybackend.post.repository.*;
import com.net.cardo.xcopybackend.post.repository.projection.*;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(PostFeedBatchIntegrationTests.QueryCounterConfig.class)
class PostFeedBatchIntegrationTests {
    static class Commands implements CommandListener {
        final Map<String,AtomicInteger> counts = new ConcurrentHashMap<>();
        @Override public void commandStarted(CommandStartedEvent event) {
            counts.computeIfAbsent(event.getCommandName(),key->new AtomicInteger()).incrementAndGet();
        }
        int count(String name) { return counts.getOrDefault(name,new AtomicInteger()).get(); }
    }
    @TestConfiguration static class QueryCounterConfig {
        @Bean Commands commands() { return new Commands(); }
        @Bean MongoClientSettingsBuilderCustomizer queryCounter(Commands commands) {
            return builder->builder.addCommandListener(commands);
        }
    }
    @Autowired PostService service;
    @Autowired PostRepository posts;
    @Autowired LikeRepository likes;
    @Autowired CommentRepository comments;
    @Autowired UserRepository users;
    @Autowired MongoTemplate mongo;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired Commands commands;
    private User author;
    private List<Post> page;
    private String outsidePost;
    private String viewer;

    @BeforeEach void fixture() {
        author = new User();author.setEmail(UUID.randomUUID()+"@batch.example.test");author.setDisplayName("Batch author");
        author = users.save(author);viewer = UUID.randomUUID().toString();
        var now = LocalDateTime.now();
        page = posts.saveAll(IntStream.range(0,20).mapToObj(i->{
            var post = new Post();post.setUserId(author.getId().toString());
            post.setContent("Batch "+i);post.setCreatedAt(now.minusSeconds(i));return post;
        }).toList());
        outsidePost = "outside-"+UUID.randomUUID();
        saveLike(page.get(0).getId(),viewer);saveLike(page.get(0).getId(),"another-viewer");
        saveLike(page.get(1).getId(),"another-viewer");saveLike(outsidePost,viewer);
        saveComment(page.get(0).getId());saveComment(page.get(0).getId());saveComment(outsidePost);
    }
    void saveLike(String postId,String userId) {
        var like = new Like();like.setPostId(postId);like.setUserId(userId);likes.save(like);
    }
    void saveComment(String postId) {
        var comment = new Comment();comment.setPostId(postId);comment.setUserId(viewer);
        comment.setContent("test");comments.save(comment);
    }
    @AfterEach void cleanup() {
        if(page==null)return;
        var ids = new ArrayList<>(page.stream().map(Post::getId).toList());ids.add(outsidePost);
        mongo.remove(Query.query(Criteria.where("postId").in(ids)),Like.class);
        mongo.remove(Query.query(Criteria.where("postId").in(ids)),Comment.class);
        posts.deleteAll(page);users.deleteById(author.getId());
    }
    @Test void coldTwentyPostPageExecutesFourMongoReadsAndOneSqlRead() {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();commands.counts.clear();
        var result = service.getUserPosts(author.getId().toString(),viewer,0,20);
        assertEquals(20,result.size());
        assertEquals(page.stream().map(Post::getId).toList(),result.stream().map(p->p.id()).toList());
        assertEquals(2,result.get(0).likesCount());assertEquals(2,result.get(0).commentsCount());
        assertTrue(result.get(0).likedByMe());assertEquals(1,result.get(1).likesCount());
        assertFalse(result.get(1).likedByMe());assertEquals(0,result.get(2).likesCount());
        assertEquals("Batch author",result.get(0).displayName());
        assertEquals(1,commands.count("find"));
        assertEquals(3,commands.count("aggregate"));
        assertEquals(0,commands.count("getMore"));
        assertEquals(1,statistics.getPrepareStatementCount());
    }
    @Test void realAggregationBoundsResultsToThePageAndKeepsViewerIsolation() {
        var ids = page.stream().map(Post::getId).toList();
        assertEquals(Set.of(new PostCount(page.get(0).getId(),2),new PostCount(page.get(1).getId(),1)),
                new HashSet<>(likes.countForPosts(ids)));
        assertEquals(List.of(new PostCount(page.get(0).getId(),2)),comments.countForPosts(ids));
        // The startup migration now prevents duplicate pairs at the database boundary.
        assertThrows(org.springframework.dao.DuplicateKeyException.class,
                () -> saveLike(page.get(0).getId(),viewer));
        assertEquals(List.of(new LikedPostId(page.get(0).getId())),likes.findLikedPostIds(ids,viewer));
        assertEquals(2,likes.findLikedPostIds(ids,"another-viewer").size());
        assertEquals(List.of(),likes.findLikedPostIds(ids,"unknown-viewer"));
    }
}
