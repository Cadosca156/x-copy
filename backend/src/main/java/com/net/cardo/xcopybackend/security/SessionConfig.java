package com.net.cardo.xcopybackend.security;

import jakarta.servlet.DispatcherType;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.SessionRepositoryFilter;

@Configuration
@EnableRedisIndexedHttpSession(redisNamespace = "xcopy:sessions:v1", maxInactiveIntervalInSeconds = 1800,
        flushMode = FlushMode.IMMEDIATE, saveMode = SaveMode.ON_SET_ATTRIBUTE)
public class SessionConfig {
    @Bean
    public SecurityContextRepository securityContextRepository() {
        var repository = new HttpSessionSecurityContextRepository();
        repository.setDisableUrlRewriting(true);
        return repository;
    }

    @Bean
    public CookieSerializer cookieSerializer(@Value("${app.auth.secure-cookies:true}") boolean secure) {
        var cookie = new DefaultCookieSerializer();
        cookie.setCookieName(BrowserSessionService.COOKIE);
        cookie.setCookiePath("/"); // OAuth authorization and callback routes also need the session.
        cookie.setUseHttpOnlyCookie(true);
        cookie.setUseSecureCookie(secure);
        cookie.setSameSite("Lax");
        cookie.setUseBase64Encoding(false);
        cookie.setCookieMaxAge(86400);
        return cookie;
    }

    @Bean
    public SessionLifetimeFilter sessionLifetimeFilter() {
        return new SessionLifetimeFilter(Clock.systemUTC());
    }

    @Bean
    public FilterRegistrationBean<SessionLifetimeFilter> sessionLifetimeRegistration(SessionLifetimeFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Run only inside Spring Security, after context loading.
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SessionAvailabilityFilter> sessionAvailabilityRegistration() {
        var registration = new FilterRegistrationBean<>(new SessionAvailabilityFilter());
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER - 1);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
        return registration;
    }
}
