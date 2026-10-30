package com.net.cardo.xcopybackend.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataAccessException;
@Component @RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final EphemeralAuthStore store;
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        try {
            if("POST".equals(req.getMethod()) && req.getRequestURI().startsWith("/api/auth/")) {
                String path=req.getRequestURI();
                boolean forgot=path.endsWith("/forgot");
                store.limit("auth-ip:"+path+":"+req.getRemoteAddr(),forgot?5:30,Duration.ofMinutes(forgot?15:5));
            }
        } catch(ResponseStatusException e) {
            res.setHeader("Retry-After","300"); res.sendError(e.getStatusCode().value(),"Забагато спроб"); return;
        } catch(DataAccessException unavailable) {
            res.sendError(503,"Вхід тимчасово недоступний"); return;
        }
        chain.doFilter(req,res);
    }
}
