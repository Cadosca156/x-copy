package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OAuthExchangeSecurityTest {
    private final JwtService jwt = mock(JwtService.class);
    private final EphemeralAuthStore store = mock(EphemeralAuthStore.class);
    private final BrowserSessionService sessions = new BrowserSessionService(jwt,store,mock(UserRepository.class));
    @Test void googleCodeWithoutTheInitiatingBrowserCookieCannotAuthenticate() {
        String code = "a".repeat(43);
        when(store.take("security:oauth-code:"+EphemeralAuthStore.digest(code))).thenReturn("browser|jwt");
        var response = new MockHttpServletResponse();
        assertThrows(ResponseStatusException.class,()->sessions.exchange(code,new MockHttpServletRequest(),response));
        assertNull(response.getHeader("Set-Cookie"));verifyNoInteractions(jwt);
    }
    @Test void sessionRevokedBetweenGoogleCallbackAndExchangeCannotAuthenticate() {
        String code = "b".repeat(43);
        when(store.take("security:oauth-code:"+EphemeralAuthStore.digest(code))).thenReturn("browser|jwt");
        when(jwt.validateToken("jwt")).thenReturn(false);
        var request = new MockHttpServletRequest();request.setCookies(new Cookie(BrowserSessionService.BINDING,"browser"));
        var response = new MockHttpServletResponse();
        assertThrows(ResponseStatusException.class,()->sessions.exchange(code,request,response));
        assertNull(response.getHeader("Set-Cookie"));
    }
}
