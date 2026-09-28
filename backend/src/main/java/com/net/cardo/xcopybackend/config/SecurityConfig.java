package com.net.cardo.xcopybackend.config;


import com.net.cardo.xcopybackend.handler.OAuth2AuthenticationSuccessHandler;
import com.net.cardo.xcopybackend.service.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomOidcUserService customOidcUserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

        .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)

                        )

                        .successHandler((request, response, authentication) -> {


                            OAuth2User user = (OAuth2User) authentication.getPrincipal();


                            oAuth2AuthenticationSuccessHandler.onAuthenticationSuccess(
                                    request,
                                    response,
                                    authentication
                            );
                        })


                        .failureHandler((request, response, exception) -> {
                            System.out.println("🔥 OAUTH ERROR:");
                            System.out.println("SESSION ID: " + request.getSession().getId());
                            System.out.println("REQUEST URL: " + request.getRequestURL());
                            exception.printStackTrace();
                            response.sendRedirect("http://localhost:5173/login?error");
                        })
                );

        return http.build();
    }
}
