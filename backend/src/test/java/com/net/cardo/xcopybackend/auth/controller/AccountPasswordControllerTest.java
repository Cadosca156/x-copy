package com.net.cardo.xcopybackend.auth.controller;

import com.net.cardo.xcopybackend.auth.service.AccountPasswordService;
import com.net.cardo.xcopybackend.auth.service.AccountPasswordService.PasswordStatus;
import com.net.cardo.xcopybackend.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountPasswordControllerTest {
    private final AccountPasswordService service = mock(AccountPasswordService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AccountPasswordController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void anonymousRequestsCannotReadOrSetAPassword() throws Exception {
        mvc.perform(get("/api/users/me/password")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/me/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"new-password\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void setsThePasswordForTheAuthenticatedIdentity() throws Exception {
        when(service.setInitialPassword("owner@example.test", "new-password")).thenReturn(new PasswordStatus(true));
        var principal = new UsernamePasswordAuthenticationToken("owner@example.test", null, List.of());
        mvc.perform(post("/api/users/me/password").principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"new-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passwordSet").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
        verify(service).setInitialPassword("owner@example.test", "new-password");
    }
}
