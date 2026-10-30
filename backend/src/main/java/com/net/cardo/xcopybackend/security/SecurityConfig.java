package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.net.cardo.xcopybackend.auth.service.CustomOidcUserService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final CustomOidcUserService oidcUserService;
    private final JwtFilter jwtFilter;
    private final AuthRateLimitFilter rateLimitFilter;
    @Value("${app.auth.secure-cookies:true}")
    private boolean secureCookies;

    // These filters must run at their configured positions inside Spring Security only.
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtRegistration(JwtFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> rateRegistration(AuthRateLimitFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var csrfRepository = new CookieCsrfTokenRepository();
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(secureCookies).sameSite("Lax").path("/"));
        http.csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**", "/media/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, failure) ->
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(info -> info.oidcUserService(oidcUserService))
                .successHandler(successHandler))
            .addFilterBefore(rateLimitFilter, OAuth2AuthorizationRequestRedirectFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
