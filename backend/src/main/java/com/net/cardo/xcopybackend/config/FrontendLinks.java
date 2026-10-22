package com.net.cardo.xcopybackend.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class FrontendLinks {
    private final String baseUrl;

    public FrontendLinks(@Value("${app.frontend-url:http://localhost:5173}") String baseUrl) {
        URI uri = URI.create(baseUrl.trim());
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
            throw new IllegalArgumentException("app.frontend-url must be an HTTP(S) origin");
        }
        this.baseUrl = baseUrl.trim().replaceAll("/+$", "");
    }

    public String oauthSuccess(String token) {
        return link("/oauth2/success", token);
    }

    public String resetPassword(String token) {
        return link("/reset-password", token);
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(baseUrl).path(path)
                .queryParam("token", "{token}").encode().buildAndExpand(token).toUriString();
    }
}
