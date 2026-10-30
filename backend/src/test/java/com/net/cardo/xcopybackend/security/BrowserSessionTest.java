package com.net.cardo.xcopybackend.security;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BrowserSessionTest {
    private final JwtService jwt=mock(JwtService.class);
    private final EphemeralAuthStore store=mock(EphemeralAuthStore.class);
    private final UserRepository users=mock(UserRepository.class);
    private final BrowserSessionService sessions=new BrowserSessionService(jwt,store,users);
    @Test void sessionCookieIsSecureHttpOnlyAndNeverReturnedInJson() throws Exception {
        var response=new MockHttpServletResponse();sessions.authenticate(response,"signed-token");
        String cookie=response.getHeader("Set-Cookie");
        assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Lax"));assertTrue(cookie.contains("Path=/api"));
        assertEquals("",response.getContentAsString());assertEquals("no-store",response.getHeader("Cache-Control"));
    }
    @Test void exchangeRequiresTheBrowserBindingAndConsumesCode() {
        String code="a".repeat(43), key="security:oauth-code:"+EphemeralAuthStore.digest(code);
        var request=new MockHttpServletRequest();request.setCookies(new Cookie(BrowserSessionService.BINDING,"right-browser"));
        when(store.take(key)).thenReturn("right-browser|jwt").thenReturn(null);when(jwt.validateToken("jwt")).thenReturn(true);
        var response=new MockHttpServletResponse();sessions.exchange(code,request,response);
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(c->c.startsWith("XCOPY_SESSION=jwt")));
        assertThrows(ResponseStatusException.class,()->sessions.exchange(code,request,new MockHttpServletResponse()));
    }
    @Test void codeFromAnotherBrowserCannotCreateASession() {
        String code="b".repeat(43);
        when(store.take("security:oauth-code:"+EphemeralAuthStore.digest(code))).thenReturn("other-browser|jwt");
        var request=new MockHttpServletRequest();request.setCookies(new Cookie(BrowserSessionService.BINDING,"my-browser"));
        var response=new MockHttpServletResponse();
        assertThrows(ResponseStatusException.class,()->sessions.exchange(code,request,response));
        assertNull(response.getHeader("Set-Cookie"));verifyNoInteractions(jwt);
    }
    @Test void logoutRevokesTokensAndClearsTheCookie() {
        var response=new MockHttpServletResponse();
        sessions.logout(new UsernamePasswordAuthenticationToken("me@example.test",null,List.of()),new MockHttpServletRequest(),response);
        verify(users).invalidateSessions("me@example.test");
        assertTrue(response.getHeader("Set-Cookie").contains("Max-Age=0"));
    }
}


