package com.net.cardo.xcopybackend.auth.handler;
import com.net.cardo.xcopybackend.security.BrowserSessionService;
import com.net.cardo.xcopybackend.config.FrontendLinks;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final BrowserSessionService sessions;
    private final FrontendLinks links;
    @Override public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication) throws IOException {
        var principal=(OAuth2User)authentication.getPrincipal();
        var session=request.getSession(false);
        String binding=session==null?null:(String)session.getAttribute("oauthBinding");
        String code=sessions.issueOAuthCode(principal.getAttribute("email"),binding);
        if(session!=null)session.invalidate();
        SecurityContextHolder.clearContext();
        response.setHeader("Cache-Control","no-store");
        response.setHeader("Referrer-Policy","no-referrer");
        response.sendRedirect(links.oauthSuccess(code));
    }
}
