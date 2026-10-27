package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.auth.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtFilterTest {
    private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);
    private final JwtService jwt = mock(JwtService.class);
    private final JwtFilter filter = new JwtFilter(users, jwt);
    private final FilterChain chain = mock(FilterChain.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void cleanUp() { SecurityContextHolder.clearContext(); }

    private void validToken() {
        request.addHeader("Authorization", "Bearer test-token");
        when(jwt.extractEmail("test-token")).thenReturn("user@example.test");
        when(jwt.validateToken("test-token")).thenReturn(true);
    }

    @Test
    void deletedUserSessionReturnsUnauthorizedWithoutCallingPosts() throws Exception {
        validToken();
        when(users.loadUserByUsername("user@example.test")).thenThrow(new UsernameNotFoundException("Deleted"));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }

    @Test
    void malformedTokenReturnsUnauthorized() throws Exception {
        request.addHeader("Authorization", "Bearer invalid");
        when(jwt.extractEmail("invalid")).thenThrow(new io.jsonwebtoken.MalformedJwtException("Invalid token"));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain, users);
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        request.addHeader("Authorization", "Bearer expired");
        when(jwt.extractEmail("expired")).thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "Expired"));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain, users);
    }

    @Test
    void validSessionAuthenticatesAndContinues() throws Exception {
        validToken();
        when(users.loadUserByUsername("user@example.test")).thenReturn(
                org.springframework.security.core.userdetails.User.withUsername("user@example.test")
                        .password("hash").authorities("USER").build());
        filter.doFilter(request, response, chain);
        assertEquals("user@example.test", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(request, response);
    }

    @Test
    void requestsWithoutBearerTokenContinueToSecurityRules() throws Exception {
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(users, jwt);
    }

    @Test
    void downstreamFailuresAreNotConvertedToSessionErrors() throws Exception {
        doThrow(new IllegalArgumentException("Business failure")).when(chain).doFilter(request, response);
        assertThrows(IllegalArgumentException.class, () -> filter.doFilter(request, response, chain));
        assertNotEquals(401, response.getStatus());
    }
}
