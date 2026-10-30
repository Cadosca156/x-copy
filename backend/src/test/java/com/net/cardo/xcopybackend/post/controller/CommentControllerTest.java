package com.net.cardo.xcopybackend.post.controller;

import com.net.cardo.xcopybackend.post.entity.Comment;
import com.net.cardo.xcopybackend.post.service.CommentService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentControllerTest {
    @Test
    void resolvesCurrentNamesAndPreservesCommentsFromMissingAuthors() {
        var service = mock(CommentService.class);
        var users = mock(UserRepository.class);
        var controller = new CommentController(service, users);
        var id = UUID.randomUUID();
        var user = new User();
        user.setId(id);
        user.setUsername("handle");
        user.setDisplayName("Current name");
        var first = new Comment();
        first.setUserId(id.toString());
        first.setContent("First comment");
        var second = new Comment();
        second.setUserId(id.toString());
        var missing = new Comment();
        missing.setUserId(UUID.randomUUID().toString());
        var legacy = new Comment();
        legacy.setUserId("invalid");
        var noId = new Comment();
        when(service.getComments("post",0,20)).thenReturn(List.of(first, second, missing, legacy, noId));
        when(users.findAllById(any())).thenReturn(List.of(user));

        var result = controller.getComments("post",0,20).getBody();

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("Current name", result.get(0).displayName());
        assertEquals("handle", result.get(0).username());
        assertEquals("First comment", result.get(0).content());
        assertEquals("Current name", result.get(1).displayName());
        for (int i = 2; i < 5; i++) {
            assertEquals("Видалений користувач", result.get(i).displayName());
        }
        verify(users).findAllById(List.of(id, UUID.fromString(missing.getUserId())));
    }
}
