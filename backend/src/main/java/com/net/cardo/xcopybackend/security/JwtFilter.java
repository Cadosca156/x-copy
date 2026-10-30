package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.auth.service.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Public authentication routes must remain usable after logout/reset/expiry.
        if(request.getRequestURI().startsWith("/api/auth/")) { filterChain.doFilter(request,response); return; }
        String header = request.getHeader("Authorization");
        String cookie = java.util.Arrays.stream(request.getCookies()==null?new jakarta.servlet.http.Cookie[0]:request.getCookies())
                .filter(c->BrowserSessionService.COOKIE.equals(c.getName())).map(jakarta.servlet.http.Cookie::getValue).findFirst().orElse(null);
        if ((header == null || !header.startsWith("Bearer ")) && cookie == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = header != null && header.startsWith("Bearer ") ? header.substring(7) : cookie;
        try {
            String email = jwtService.extractEmail(jwt);
            if (email == null || email.isBlank() || !jwtService.validateToken(jwt)) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(email);
                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException invalidSession) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
