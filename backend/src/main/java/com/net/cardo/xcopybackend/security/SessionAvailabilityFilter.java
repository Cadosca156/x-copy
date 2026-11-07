package com.net.cardo.xcopybackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Session reads/commits happen outside MVC, so ControllerAdvice cannot handle their failures. */
public class SessionAvailabilityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(SessionAvailabilityFilter.class);

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (DataAccessException unavailable) {
            SecurityContextHolder.clearContext();
            if (response.isCommitted()) {
                log.warn("Session storage failed after response commit: {}", unavailable.getClass().getSimpleName());
                return;
            }
            response.resetBuffer();
            response.setStatus(503);
            response.setHeader("Retry-After", "2");
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Сесії тимчасово недоступні. Спробуйте пізніше.\",\"status\":503,\"timestamp\":"
                    + System.currentTimeMillis() + "}");
        }
    }
}
