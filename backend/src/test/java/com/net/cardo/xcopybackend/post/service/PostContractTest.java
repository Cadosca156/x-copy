package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.exception.exceptions.*;

import com.net.cardo.xcopybackend.exception.GlobalExceptionHandler;
import com.net.cardo.xcopybackend.post.controller.PostController;
import com.net.cardo.xcopybackend.post.dto.response.*;
import com.net.cardo.xcopybackend.post.repository.*;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostContractTest {
    @Test void textBoundariesAndWhitespaceMatchTheBrowser() {
        assertEquals(280,PostRules.MAX_CONTENT_LENGTH);
        assertEquals("hello",PostRules.content(" \u00a0hello\uFEFF ",null));
        assertEquals(280,PostRules.content("😀".repeat(140),null).length());
        assertThrows(InvalidPostException.class,()->PostRules.content("😀".repeat(141),null));
        assertThrows(InvalidPostException.class,()->PostRules.content(" \u00a0\uFEFF ",null));
        assertThrows(InvalidPostException.class,()->PostRules.content("",new MockMultipartFile("file",new byte[0])));
        assertEquals("",PostRules.content(null,new MockMultipartFile("file",new byte[]{1})));
    }
    @Test void serviceRejectsEmptyAndOversizedContentBeforeAnyStorageOrRepositoryWork() {
        var posts=mock(PostRepository.class);var likes=mock(LikeRepository.class);var comments=mock(CommentRepository.class);
        var users=mock(UserRepository.class);var media=mock(MediaStorageService.class);var cache=mock(FeedCacheService.class);
        var service=new PostService(posts,likes,comments,users,media,cache);
        for(String text:new String[]{null,""," ","x".repeat(281)})
            assertThrows(InvalidPostException.class,()->service.createPost(UUID.randomUUID(),text,null));
        verifyNoInteractions(posts,likes,comments,users,media,cache);
    }
    @Test void createAndFeedSerializeTheSameSharedPostResponseFixture() throws Exception {
        var user=new User();user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var users=mock(UserRepository.class);when(users.findByEmail("alice@example.test")).thenReturn(Optional.of(user));
        var service=mock(PostService.class);
        var dto=new PostResponse("contract-post",user.getId().toString(),"alice","Alice","Contract post",null,null,
                LocalDateTime.of(2026,11,7,12,34,56),2,3,false,null);
        when(service.createPost(eq(user.getId()),anyString(),isNull())).thenReturn(dto);
        when(service.getPostWindow(user.getId().toString(),null,20)).thenReturn(new PostWindow(List.of(dto),null,false));
        var mvc=MockMvcBuilders.standaloneSetup(new PostController(service,users,mock(LikeService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        String expected;
        try(var resource=Objects.requireNonNull(getClass().getResourceAsStream("/post-response.json"))){expected=new String(resource.readAllBytes(),StandardCharsets.UTF_8);}
        var principal=new UsernamePasswordAuthenticationToken("alice@example.test",null,List.of());
        mvc.perform(multipart("/api/posts").param("content","Contract post").principal(principal))
                .andExpect(status().isCreated()).andExpect(content().json(expected));
        mvc.perform(get("/api/posts").param("pagination","cursor").principal(principal))
                .andExpect(status().isOk()).andExpect(content().json("{\"items\":["+expected+"],\"nextCursor\":null,\"hasMore\":false}"));
    }
}
