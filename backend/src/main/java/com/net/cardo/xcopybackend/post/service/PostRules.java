package com.net.cardo.xcopybackend.post.service;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public final class PostRules {
    public static final int MAX_CONTENT_LENGTH=280;
    private PostRules() {}
    public static String content(String content,MultipartFile file) {
        String raw=content==null?"":content;
        if(raw.length()>MAX_CONTENT_LENGTH)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Текст допису має містити до 280 символів");
        // ECMAScript trim whitespace: match the browser without counting code points differently.
        String normalized=raw.replaceAll("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$","");
        if(normalized.isEmpty() && (file==null || file.isEmpty()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Додай текст, фото або відео");
        return normalized;
    }
}
