package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.auth.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor

public class JwtFilter extends OncePerRequestFilter {
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authorizationHeader =
                request.getHeader("Authorization");

        System.out.println("===== JWT FILTER =====");
        System.out.println("REQUEST: " + request.getRequestURI());
        System.out.println("HEADER: " + authorizationHeader);

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            System.out.println("NO BEARER TOKEN");

            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authorizationHeader.substring(7);

        System.out.println("JWT: " + jwt);

        String email = jwtService.extractEmail(jwt);

        System.out.println("EMAIL FROM JWT: " + email);

        if (email != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            System.out.println(
                    "USER FOUND: " + userDetails.getUsername()
            );

            boolean valid = jwtService.validateToken(jwt);

            System.out.println("JWT VALID: " + valid);

            if (valid) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

                System.out.println(
                        "AUTHENTICATED: " +
                                SecurityContextHolder
                                        .getContext()
                                        .getAuthentication()
                                        .getName()
                );
            }
        }

        filterChain.doFilter(request, response);
    }


   }


