package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.net.cardo.xcopybackend.auth.service.CustomOidcUserService;
import com.net.cardo.xcopybackend.exception.GlobalExceptionHandler;
import com.net.cardo.xcopybackend.twofactor.controller.TwoFactorCodeController;
import com.net.cardo.xcopybackend.twofactor.service.TwoFactorService;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.context.*;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
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
    @Import({SecurityConfig.class, AuthRateLimitFilter.class, BrowserSessionService.class,
            BrowserAuthController.class, TwoFactorCodeController.class, Probe.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean EphemeralAuthStore store() { return mock(EphemeralAuthStore.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean TwoFactorService twoFactor() { return mock(TwoFactorService.class); }
        @Bean CustomOidcUserService oidc() { return mock(CustomOidcUserService.class); }
        @Bean OAuth2AuthenticationSuccessHandler success() { return mock(OAuth2AuthenticationSuccessHandler.class); }
        @Bean SecurityContextRepository contexts() { return new HttpSessionSecurityContextRepository(); }
        @Bean SessionLifetimeFilter lifetime() { return new SessionLifetimeFilter(Clock.systemUTC()); }
        @Bean @SuppressWarnings("unchecked") FindByIndexNameSessionRepository<Session> sessions() { return mock(FindByIndexNameSessionRepository.class); }
        @Bean ClientRegistrationRepository clients() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("google")
                    .clientId("test").clientSecret("test").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://backend.test/login/oauth2/code/google")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                    .tokenUri("https://oauth2.googleapis.com/token").userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                    .userNameAttributeName("sub").scope("openid", "email").build());
        }
    }

    @RestController static class Probe {
        @GetMapping("/api/probe") public Map<String, String> probe(java.security.Principal user) { return Map.of("email", user.getName()); }
    }

    @Autowired WebApplicationContext context;
    @Autowired TwoFactorService twoFactor;
    @Autowired UserRepository users;
    @Autowired EphemeralAuthStore store;
    MockMvc mvc;
    private com.net.cardo.xcopybackend.user.entity.User user;
    record Csrf(Cookie cookie, String token) {}

    @BeforeEach void setup() {
        reset(twoFactor, users, store);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
        user = new com.net.cardo.xcopybackend.user.entity.User();user.setId(UUID.randomUUID());user.setEmail("me@example.test");
        when(users.findForAuthentication(user.getId())).thenReturn(Optional.of(user));
        when(twoFactor.verifyCode("challenge", "123456")).thenReturn(LoginProof.from(user));
    }

    Csrf csrf() throws Exception {
        var response = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        var matcher = java.util.regex.Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.getContentAsString());
        assertTrue(matcher.find());return new Csrf(response.getCookie("XSRF-TOKEN"), matcher.group(1));
    }

    MockHttpSession login() throws Exception {
        var csrf = csrf();
        var result = mvc.perform(post("/api/auth/verify-2fa").cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType("application/json").content("{\"challengeId\":\"challenge\",\"code\":\"123456\"}"))
                .andExpect(status().isOk()).andExpect(content().json("{\"authenticated\":true}")).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test void loginRequiresCsrfAndPersistsSessionWithoutReturningCredentials() throws Exception {
        mvc.perform(post("/api/auth/verify-2fa").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(twoFactor);
        var session = login();assertNotNull(session);
        clearInvocations(users);
        mvc.perform(get("/api/probe").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail()));
        verifyNoInteractions(users); // Authentication restoration does not query PostgreSQL.
    }

    @Test void retiredJwtCookiesAndBearerTokensCannotAuthenticate() throws Exception {
        mvc.perform(get("/api/probe").cookie(new Cookie("XCOPY_SESSION", "old.jwt.token"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/probe").header("Authorization", "Bearer old.jwt.token")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/csrf").cookie(new Cookie(BrowserSessionService.COOKIE, "expired"))).andExpect(status().isOk());
        verifyNoInteractions(users);
    }

    @Test void logoutRequiresCsrfAndInvalidatesOnlyThisBrowserSession() throws Exception {
        var session = login();var other = login();clearInvocations(users);
        mvc.perform(post("/api/users/me/logout").session(session)).andExpect(status().isForbidden());
        var csrf = csrf();
        var response = mvc.perform(post("/api/users/me/logout").session(session).cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        assertTrue(session.isInvalid());
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(c -> c.startsWith(BrowserSessionService.COOKIE + "=") && c.contains("Max-Age=0")));
        mvc.perform(get("/api/probe").session(other)).andExpect(status().isOk());
        verifyNoInteractions(users);
    }

    @Test void googleCannotBeStartedWithAnUnboundUrl() throws Exception {
        mvc.perform(get("/oauth2/authorization/google")).andExpect(status().isBadRequest());
    }
}
