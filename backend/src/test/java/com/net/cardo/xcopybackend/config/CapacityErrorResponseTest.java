package com.net.cardo.xcopybackend.config;

import com.net.cardo.xcopybackend.exception.GlobalExceptionHandler;
import com.net.cardo.xcopybackend.post.controller.PostController;
import com.net.cardo.xcopybackend.post.service.*;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CapacityErrorResponseTest {
    @Test void dependencyFailuresReturnRetryableGeneric503() throws Exception {
        var posts=mock(PostService.class);var users=mock(UserRepository.class);var user=new User();user.setId(UUID.randomUUID());
        when(users.findByEmail("viewer@example.test")).thenReturn(Optional.of(user));
        var mvc=MockMvcBuilders.standaloneSetup(new PostController(posts,users,mock(LikeService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var principal=new UsernamePasswordAuthenticationToken("viewer@example.test",null,List.of());
        for(RuntimeException failure:List.of(new org.springframework.dao.QueryTimeoutException("private SQL"),
                new org.springframework.core.task.TaskRejectedException("private queue"),
                new org.springframework.mail.MailSendException("private SMTP"))) {
            doThrow(failure).when(posts).getPosts(anyString(),anyInt(),anyInt());
            mvc.perform(get("/api/posts").principal(principal)).andExpect(status().isServiceUnavailable())
                    .andExpect(header().string("Retry-After","2"))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private"))));
        }
    }
}
