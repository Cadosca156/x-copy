package com.net.cardo.xcopybackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import com.net.cardo.xcopybackend.auth.service.CustomUserDetailsService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtFilterCapacityTest {
    @Test void databaseTimeoutIs503InsteadOfInvalidSessionOrUncaught500() throws Exception {
        var jwt=mock(JwtService.class);var details=mock(CustomUserDetailsService.class);
        when(jwt.extractEmail("token")).thenReturn("viewer@example.test");
        when(jwt.validateToken("token")).thenThrow(new org.springframework.dao.QueryTimeoutException("private SQL"));
        var request=new MockHttpServletRequest("GET","/api/posts");request.addHeader("Authorization","Bearer token");
        var response=new MockHttpServletResponse();
        try {
            new JwtFilter(details,jwt).doFilter(request,response,(a,b)->fail("Unavailable authentication must stop the request"));
            assertEquals(503,response.getStatus());assertEquals("2",response.getHeader("Retry-After"));
            assertNull(SecurityContextHolder.getContext().getAuthentication());verifyNoInteractions(details);
        } finally {SecurityContextHolder.clearContext();}
    }
}
