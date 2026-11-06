package com.net.cardo.xcopybackend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reject before security/database access or multipart parsing; never enqueue callers. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+20)
public class CapacityFilter extends OncePerRequestFilter {
    private final Semaphore requests,auth,reads,uploads,media;
    public CapacityFilter(@Value("${app.capacity.requests:32}") int requests,
            @Value("${app.capacity.auth:4}") int auth,@Value("${app.capacity.reads:12}") int reads,
            @Value("${app.capacity.uploads:4}") int uploads,@Value("${app.capacity.media:8}") int media) {
        if(requests<1 || auth<1 || reads<1 || uploads<1 || media<1
                || Math.max(Math.max(auth,reads),Math.max(uploads,media))>requests)
            throw new IllegalArgumentException("Invalid request capacity limits");
        this.requests=new Semaphore(requests);this.auth=new Semaphore(auth);
        this.reads=new Semaphore(reads);this.uploads=new Semaphore(uploads);this.media=new Semaphore(media);
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=path(request);
        return "OPTIONS".equals(request.getMethod()) || !(path.startsWith("/api/")
                || path.startsWith("/media/") || path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws IOException,ServletException {
        if(!requests.tryAcquire()) { reject(response);return; }
        Semaphore category=category(request);boolean acquired=false;
        try {
            if(category!=null) {
                acquired=category.tryAcquire();
                if(!acquired) { reject(response);return; }
            }
            chain.doFilter(request,response);
        } finally {
            if(acquired)category.release();requests.release();
        }
    }
    private Semaphore category(HttpServletRequest request) {
        String path=path(request);
        if(request.getContentType()!=null && request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data"))return uploads;
        if(path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")
                || path.startsWith("/api/auth/") && !"GET".equals(request.getMethod()))return auth;
        if(path.startsWith("/media/"))return media;
        if("GET".equals(request.getMethod()) && (path.equals("/api/posts") || path.startsWith("/api/posts/")
                || path.equals("/api/users/me/following/posts")))return reads;
        return null;
    }
    private String path(HttpServletRequest request) {
        // The container's decoded servlet path also removes matrix path parameters.
        String path=request.getServletPath();
        return path==null || path.isEmpty()?request.getRequestURI().substring(request.getContextPath().length()):path;
    }
    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(503);response.setHeader("Retry-After","2");response.setHeader("Cache-Control","no-store");
        response.setContentType("application/json");response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"Сервер зайнятий. Спробуйте трохи пізніше.\",\"status\":503}");
    }
}
