package com.net.cardo.xcopybackend.security;

import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {
    private final EphemeralAuthStore store = mock(EphemeralAuthStore.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(store);
    @Test void excessLoginAttemptsNeverReachTheLoginController() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        var response = new MockHttpServletResponse();
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(store)
            .limit("auth-ip:/api/auth/login:127.0.0.1",30,Duration.ofMinutes(5));
        filter.doFilter(request,response,chain);
        assertEquals(429,response.getStatus()); assertNotNull(response.getHeader("Retry-After"));
        verifyNoInteractions(chain);
    }
    @Test void redisFailureDoesNotAllowAuthenticationToBypassTheLimit() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/password/forgot");
        var response = new MockHttpServletResponse();
        doThrow(new DataAccessResourceFailureException("offline")).when(store)
            .limit("auth-ip:/api/auth/password/forgot:127.0.0.1",5,Duration.ofMinutes(15));
        filter.doFilter(request,response,chain);
        assertEquals(503,response.getStatus()); verifyNoInteractions(chain);
    }
    @Test void mediaWritesAreThrottledBeforeTheUploadController() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/posts");
        var response = new MockHttpServletResponse();
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(store)
            .limit("write-ip:127.0.0.1",60,Duration.ofMinutes(1));
        filter.doFilter(request,response,chain);
        assertEquals(429,response.getStatus()); verifyNoInteractions(chain);
    }
    @Test void expiredGoogleStartCannotCreateAnOAuthSession() throws Exception {
        var request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        request.addParameter("app_login","a".repeat(43));
        var response = new MockHttpServletResponse();
        filter.doFilter(request,response,chain);
        assertEquals(400,response.getStatus()); assertNull(request.getSession(false));
        verifyNoInteractions(chain);
    }
}
