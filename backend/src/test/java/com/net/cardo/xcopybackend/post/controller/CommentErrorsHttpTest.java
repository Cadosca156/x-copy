package com.net.cardo.xcopybackend.post.controller;

import com.net.cardo.xcopybackend.exception.GlobalExceptionHandler;
import com.net.cardo.xcopybackend.post.entity.Comment;
import com.net.cardo.xcopybackend.post.repository.CommentRepository;
import com.net.cardo.xcopybackend.post.repository.PostRepository;
import com.net.cardo.xcopybackend.post.service.CommentService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentErrorsHttpTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new CommentController(new CommentService(comments, posts), users))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("alice@example.test", null, List.of());

    private User user() {
        var user = new User(); user.setId(UUID.randomUUID());
        when(users.findByEmail(auth.getName())).thenReturn(Optional.of(user));
        return user;
    }

    @Test void deletedAccountAndMissingPrincipalReturn401InsteadOf500() throws Exception {
        mvc.perform(post("/api/posts/post/comments").principal(auth).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Увійдіть в акаунт знову"));
        mvc.perform(delete("/api/posts/post/comments/comment"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(comments, posts);
    }

    @Test void missingPostAndEmptyCommentHaveDifferentDomainMessages() throws Exception {
        user();
        mvc.perform(post("/api/posts/post/comments").principal(auth).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hello\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Допис не знайдено"));
        when(posts.existsActiveById("post")).thenReturn(true);
        mvc.perform(post("/api/posts/post/comments").principal(auth).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Коментар має містити від 1 до 5000 символів"));
        verifyNoInteractions(comments);
    }

    @Test void deletingAnotherUsersCommentIs403AndWrongParentIs404() throws Exception {
        user();
        var comment = new Comment();comment.setId("comment");comment.setPostId("post");comment.setUserId("other-user");
        when(comments.findById("comment")).thenReturn(Optional.of(comment));
        mvc.perform(delete("/api/posts/post/comments/comment").principal(auth))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Можна видаляти лише власні коментарі"));
        mvc.perform(delete("/api/posts/wrong-post/comments/comment").principal(auth))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Коментар не знайдено"));
        mvc.perform(delete("/api/posts/post/comments/missing").principal(auth))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Коментар не знайдено"));
        verify(comments, never()).delete(any());
    }
}
