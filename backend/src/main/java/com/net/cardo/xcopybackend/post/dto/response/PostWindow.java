package com.net.cardo.xcopybackend.post.dto.response;
import java.util.List;

public record PostWindow(List<PostResponse> items,String nextCursor,boolean hasMore) {}
