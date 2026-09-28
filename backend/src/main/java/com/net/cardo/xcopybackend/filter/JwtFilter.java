package com.net.cardo.xcopybackend.filter;

import com.net.cardo.xcopybackend.service.CustomUserDetailsService;
import com.net.cardo.xcopybackend.service.JwtService;

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
       final String authorizationHeader = request.getHeader("Authorization");
       if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
           filterChain.doFilter(request, response);
           return;
       }
    String jwt = authorizationHeader.substring(7);
      String email = jwtService.extractEmail(jwt);
      if (email != null &&
      SecurityContextHolder.getContext().getAuthentication() == null){

          UserDetails userDetails = userDetailsService.loadUserByUsername(email);


          if(jwtService.validateToken(jwt)){

              UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());


              authentication.setDetails( new WebAuthenticationDetailsSource().buildDetails(request) );

              SecurityContextHolder.getContext().setAuthentication(authentication);
          }
      }

filterChain.doFilter(request, response);


   }

}
