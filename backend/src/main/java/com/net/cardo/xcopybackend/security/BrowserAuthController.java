package com.net.cardo.xcopybackend.security;
import jakarta.servlet.http.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class BrowserAuthController {
    private final BrowserSessionService sessions;
    public record Exchange(String code) {}
    @GetMapping("/api/auth/csrf")
    public Map<String,String> csrf(CsrfToken token,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @PostMapping("/api/auth/oauth/start")
    public Map<String,String> start(HttpServletResponse response) { return Map.of("startId",sessions.beginOAuth(response)); }
    @PostMapping("/api/auth/oauth/exchange")
    public Map<String,Boolean> exchange(@RequestBody Exchange body,HttpServletRequest request,HttpServletResponse response) {
        sessions.exchange(body.code(),request,response);return Map.of("authenticated",true);
    }
    @PostMapping("/api/users/me/logout") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void logout(Authentication auth,HttpServletRequest request,HttpServletResponse response) { sessions.logout(auth,request,response); }
}
