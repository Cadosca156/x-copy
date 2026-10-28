package com.net.cardo.xcopybackend.config;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;
import jakarta.servlet.ServletException;
import static org.junit.jupiter.api.Assertions.*;

class ApiCorsConfigTest {
    private static final String ORIGIN = "https://main.d2s4oyee9a0d3l.amplifyapp.com";
    private final CorsFilter filter = new CorsFilter(new ApiCorsConfig()
            .corsConfigurationSource("http://localhost:5173," + ORIGIN));

    @Test
    void allowsAmplifyLogin() throws ServletException, IOException {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", ORIGIN);
        var response = new MockHttpServletResponse();
        var reachedController = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> reachedController.set(true));
        assertTrue(reachedController.get());
        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void allowsAuthenticatedProfilePreflight() throws ServletException, IOException {
        var request = new MockHttpServletRequest("OPTIONS", "/api/users/me/profile");
        request.addHeader("Origin", ORIGIN);
        request.addHeader("Access-Control-Request-Method", "PATCH");
        request.addHeader("Access-Control-Request-Headers", "authorization,content-type");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Preflight must finish before authentication"));
        assertEquals(200, response.getStatus());
        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
        assertTrue(response.getHeader("Access-Control-Allow-Methods").contains("PATCH"));
    }

    @Test
    void rejectsUntrustedOrigin() throws ServletException, IOException {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "https://untrusted.example");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Untrusted origin reached controller"));
        assertEquals(403, response.getStatus());
    }

    @Test
    void preservesLocalDevelopment() throws ServletException, IOException {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "http://localhost:5173");
        var response = new MockHttpServletResponse();
        var reachedController = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> reachedController.set(true));
        assertTrue(reachedController.get());
    }

    @Test
    void allowsFollowPutFromWebsiteAndLocalDevelopment() throws ServletException, IOException {
        for (String origin : new String[] {ORIGIN, "http://localhost:5173"}) {
            var request = new MockHttpServletRequest("PUT", "/api/users/target/follow");
            request.addHeader("Origin", origin);
            request.addHeader("Authorization", "Bearer test-token");
            var response = new MockHttpServletResponse();
            var reachedController = new AtomicBoolean();
            filter.doFilter(request, response, (req, res) -> reachedController.set(true));
            assertTrue(reachedController.get(), "Follow request must reach controller for " + origin);
            assertEquals(200, response.getStatus());
            assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"));
        }
    }

    @Test
    void allowsFollowAndUnfollowPreflight() throws ServletException, IOException {
        for (String method : new String[] {"PUT", "DELETE"}) {
            var request = new MockHttpServletRequest("OPTIONS", "/api/users/target/follow");
            request.addHeader("Origin", ORIGIN);
            request.addHeader("Access-Control-Request-Method", method);
            request.addHeader("Access-Control-Request-Headers", "authorization");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> fail("Preflight must finish before authentication"));
            assertEquals(200, response.getStatus(), method + " preflight must be allowed");
            assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
            assertTrue(response.getHeader("Access-Control-Allow-Methods").contains(method));
        }
    }

    @Test
    void rejectsFollowFromUntrustedOrigin() throws ServletException, IOException {
        var request = new MockHttpServletRequest("PUT", "/api/users/target/follow");
        request.addHeader("Origin", "https://untrusted.example");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Untrusted follow must not reach controller"));
        assertEquals(403, response.getStatus());
    }
}
