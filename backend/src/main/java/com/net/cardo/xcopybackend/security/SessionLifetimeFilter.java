package com.net.cardo.xcopybackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionLifetimeFilter extends OncePerRequestFilter {
    private final Clock clock;
    public SessionLifetimeFilter(Clock clock) { this.clock = clock; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var session = request.getSession(false);
        if (authentication != null && authentication.getPrincipal() instanceof SessionPrincipal && session != null
                && clock.millis() - session.getCreationTime() >= Duration.ofDays(1).toMillis()) {
            session.invalidate();
            SecurityContextHolder.clearContext();
        }
        // Public login/reset routes remain usable; authorization rejects protected routes after expiry.
        chain.doFilter(request, response);
    }
}
