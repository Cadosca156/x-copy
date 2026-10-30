package com.net.cardo.xcopybackend.security;
import com.net.cardo.xcopybackend.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.net.cardo.xcopybackend.auth.service.*;
import com.net.cardo.xcopybackend.twofactor.controller.TwoFactorCodeController;
import com.net.cardo.xcopybackend.twofactor.service.TwoFactorService;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(BrowserSecurityFlowTest.Config.class) @WebAppConfiguration
class BrowserSecurityFlowTest {
    @Configuration @EnableWebMvc
    @Import({SecurityConfig.class,JwtFilter.class,AuthRateLimitFilter.class,BrowserSessionService.class,
        BrowserAuthController.class,TwoFactorCodeController.class,Probe.class})
    static class Config {
        @Bean JwtService jwt(){return mock(JwtService.class);}
        @Bean EphemeralAuthStore store(){return mock(EphemeralAuthStore.class);}
        @Bean UserRepository users(){return mock(UserRepository.class);}
        @Bean CustomUserDetailsService details(){return mock(CustomUserDetailsService.class);}
        @Bean TwoFactorService twoFactor(){return mock(TwoFactorService.class);}
        @Bean CustomOidcUserService oidc(){return mock(CustomOidcUserService.class);}
        @Bean OAuth2AuthenticationSuccessHandler success(){return mock(OAuth2AuthenticationSuccessHandler.class);}
        @Bean ClientRegistrationRepository clients() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("google")
                .clientId("test").clientSecret("test").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://backend.test/login/oauth2/code/google")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token").userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub").scope("openid","email").build());
        }
    }
    @RestController static class Probe {
        @GetMapping("/api/probe") public Map<String,String> probe(java.security.Principal user){return Map.of("email",user.getName());}
    }
    @Autowired WebApplicationContext context;
    @Autowired JwtService jwt;
    @Autowired EphemeralAuthStore store;
    @Autowired TwoFactorService twoFactor;
    @Autowired CustomUserDetailsService details;
    @Autowired UserRepository users;
    MockMvc mvc;
    record Csrf(Cookie cookie,String token) {}
    @BeforeEach void setup() {
        reset(jwt,store,twoFactor,details,users);
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build();
        when(jwt.extractEmail("valid-jwt")).thenReturn("me@example.test");
        when(jwt.validateToken("valid-jwt")).thenReturn(true);
        when(details.loadUserByUsername("me@example.test")).thenReturn(User.withUsername("me@example.test").password("").authorities("USER").build());
    }
    Csrf csrf() throws Exception {
        var response=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        var matcher=java.util.regex.Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.getContentAsString());
        assertTrue(matcher.find());return new Csrf(response.getCookie("XSRF-TOKEN"),matcher.group(1));
    }
    @Test void loginRequiresCsrfAndCreatesHttpOnlyCookieWithoutLeakingJwt() throws Exception {
        mvc.perform(post("/api/auth/verify-2fa").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(twoFactor);
        var csrf=csrf();when(twoFactor.verifyCode("challenge","123456")).thenReturn("valid-jwt");
        var response=mvc.perform(post("/api/auth/verify-2fa").cookie(csrf.cookie()).header("X-XSRF-TOKEN",csrf.token())
            .contentType("application/json").content("{\"challengeId\":\"challenge\",\"code\":\"123456\"}"))
            .andExpect(status().isOk()).andReturn().getResponse();
        assertEquals("{\"authenticated\":true}",response.getContentAsString());
        String cookie=response.getHeader("Set-Cookie");
        assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("SameSite=Lax"));
        mvc.perform(get("/api/probe").cookie(new Cookie(BrowserSessionService.COOKIE,"valid-jwt"))).andExpect(status().isOk());
    }
    @Test void revokedCookieIsRejectedButDoesNotBlockStartingAnotherLogin() throws Exception {
        mvc.perform(get("/api/probe").cookie(new Cookie(BrowserSessionService.COOKIE,"expired"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/csrf").cookie(new Cookie(BrowserSessionService.COOKIE,"expired"))).andExpect(status().isOk());
    }
    @Test void logoutRequiresCsrfRevokesSessionsAndExpiresCookie() throws Exception {
        var session=new Cookie(BrowserSessionService.COOKIE,"valid-jwt");
        mvc.perform(post("/api/users/me/logout").cookie(session)).andExpect(status().isForbidden());
        var csrf=csrf();
        var response=mvc.perform(post("/api/users/me/logout").cookie(session,csrf.cookie()).header("X-XSRF-TOKEN",csrf.token()))
            .andExpect(status().isNoContent()).andReturn().getResponse();
        verify(users).invalidateSessions("me@example.test");assertTrue(response.getHeader("Set-Cookie").contains("Max-Age=0"));
        when(jwt.validateToken("valid-jwt")).thenReturn(false);
        mvc.perform(get("/api/probe").cookie(session)).andExpect(status().isUnauthorized());
    }
    @Test void googleCannotBeStartedWithAnUnboundUrl() throws Exception {
        mvc.perform(get("/oauth2/authorization/google")).andExpect(status().isBadRequest());
    }
}
