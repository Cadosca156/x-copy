package com.net.cardo.xcopybackend.security;
import java.time.Duration;
import java.util.Arrays;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
@Service @RequiredArgsConstructor
public class BrowserSessionService {
    public static final String COOKIE="XCOPY_SESSION", BINDING="XCOPY_OAUTH_BIND";
    private final JwtService jwt;
    private final EphemeralAuthStore store;
    private final UserRepository users;
    @Value("${app.auth.secure-cookies:true}") private boolean secure=true;
    private void cookie(HttpServletResponse response,String name,String value,Duration age) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(name,value).httpOnly(true).secure(secure).sameSite("Lax").path("/api").maxAge(age).build().toString());
        response.setHeader("Cache-Control","no-store");
    }
    public void authenticate(HttpServletResponse response,String token) { cookie(response,COOKIE,token,Duration.ofDays(1)); }
    public String beginOAuth(HttpServletResponse response) {
        String binding=EphemeralAuthStore.random(), start=EphemeralAuthStore.random();
        cookie(response,BINDING,binding,Duration.ofMinutes(5));
        store.put("security:oauth-start:"+EphemeralAuthStore.digest(start),binding,Duration.ofMinutes(5));
        return start;
    }
    public String issueOAuthCode(String email,String binding) {
        if(binding==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Почніть вхід через Google знову.");
        String code=EphemeralAuthStore.random();
        store.put("security:oauth-code:"+EphemeralAuthStore.digest(code),binding+"|"+jwt.generateToken(email),Duration.ofSeconds(60));
        return code;
    }
    public void exchange(String code,HttpServletRequest request,HttpServletResponse response) {
        if(code==null || !code.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        String binding=Arrays.stream(request.getCookies()==null?new Cookie[0]:request.getCookies())
                .filter(c->BINDING.equals(c.getName())).map(Cookie::getValue).findFirst().orElse("");
        String value=store.take("security:oauth-code:"+EphemeralAuthStore.digest(code));
        if(value==null || !value.startsWith(binding+"|") || binding.isBlank()) throw invalid();
        String token=value.substring(value.indexOf('|')+1);
        if(!jwt.validateToken(token)) throw invalid();
        authenticate(response,token);cookie(response,BINDING,"",Duration.ZERO);
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Сесію Google-входу завершено. Спробуйте знову."); }
    public void logout(Authentication auth,HttpServletRequest request,HttpServletResponse response) {
        String email=auth.getPrincipal() instanceof OidcUser oidc?oidc.getEmail():auth.getName();
        users.invalidateSessions(email);
        cookie(response,COOKIE,"",Duration.ZERO);
        var session=request.getSession(false);if(session!=null)session.invalidate();
        SecurityContextHolder.clearContext();
    }
}
